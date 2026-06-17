from google import genai
from PIL import Image
import io
import os
import time

class VisionService:
    def __init__(self, api_key: str):
        # Default to a free-tier vision model. Pro preview models (e.g.
        # gemini-3-pro-preview) return HTTP 429 "limit: 0" without billing
        # enabled on the key. Override via GEMINI_MODEL on a billed key.
        self.model_id = os.getenv("GEMINI_MODEL", "gemini-3.5-flash")
        # Defer client creation when no key is configured so the app can still
        # import and start (matching main.py's "Vision service will fail"
        # warning). This keeps the module importable in CI and for contributors
        # who haven't created a .env yet; get_description still fails loudly if
        # called without a key.
        self.client = genai.Client(api_key=api_key) if api_key else None

    def get_description(self, image_bytes: bytes) -> str:
        if self.client is None:
            raise RuntimeError(
                "VisionService is not configured: set GOOGLE_API_KEY in the environment."
            )
        print(f"Calling Gemini with model: {self.model_id}")
        try:
            image = Image.open(io.BytesIO(image_bytes))
        except Exception as e:
            print(f"Could not open image: {e}")
            return "I couldn't read that image. Please retake the photo and try again."
        prompt = """You are MetaHelper, an audio assistant that reads and explains code and technical content for someone looking at a screen, whiteboard, slide, or printed page who needs to consume it by ear — for example a developer or student who is blind or has low vision.

        Identify what the image contains (source code, terminal/error output, a diagram, or technical text) and detect the programming language automatically.
        If it's too blurry, cropped, or unreadable, say so plainly and ask the user to reframe and retake the photo — never guess at hidden content.

        For SOURCE CODE, give TWO layers, in this order:

        1. VERBATIM READ-OUT: read the code exactly as written so the listener can follow and transcribe it.
           - Speak symbols as words: "open brace", "close brace", "semicolon", "equals", "plus plus", "open paren", "close paren".
           - Make block structure clear (e.g. "inside the loop", "back at the top level").
           - Example: "for open-paren int i equals zero semicolon i less than ten semicolon i plus plus close-paren open-brace".

        2. EXPLANATION: in plain English, what the code does and why, block by block.
           - Use connective words like "first", "then", "this returns".
           - Example: "This is a for-loop with an integer i starting at zero that runs while i is less than ten, incrementing i each time."

        For an ERROR or TERMINAL OUTPUT: read the key message verbatim, then explain the likely cause and a concrete next step.
        For a DIAGRAM or TECHNICAL TEXT: describe its structure and meaning concisely.

        AUDIO GUIDELINES (this is read aloud by text-to-speech):
        - Plain spoken English only. Do NOT use LaTeX, Markdown, tables, or notation that doesn't speak well.
        - Keep it tight — favor clarity over completeness; the listener can ask for a re-read.
        - Don't comment on image quality unless it is actually unreadable.
        """
        # Try the configured model, then a stable fallback, retrying transient
        # errors (503 high-demand, 429, 500) with backoff. Newer flash models in
        # particular return sporadic 503s under load, which previously surfaced
        # as an instant "I had trouble analyzing that image" with no retry.
        models_to_try = [self.model_id]
        if "gemini-2.5-flash-lite" not in models_to_try:
            models_to_try.append("gemini-2.5-flash-lite")

        last_error = None
        for model in models_to_try:
            for attempt in range(3):
                try:
                    response = self.client.models.generate_content(
                        model=model, contents=[prompt, image]
                    )
                    text = getattr(response, "text", None)
                    if text:
                        return text
                    # Empty text usually means a safety block; another model
                    # would block the same content, so don't bother falling back.
                    print(f"{model}: empty response (possible safety block).")
                    return "I couldn't generate an answer for that image. Please retake the photo with the full question in view."
                except Exception as e:
                    last_error = e
                    transient = any(s in str(e) for s in (
                        "503", "UNAVAILABLE", "429", "RESOURCE_EXHAUSTED",
                        "500", "high demand", "overloaded",
                    ))
                    print(f"Gemini {model} attempt {attempt + 1} failed (transient={transient}): {e}")
                    if transient and attempt < 2:
                        time.sleep(1.5 * (attempt + 1))  # 1.5s, then 3s
                        continue
                    break  # non-transient, or out of attempts -> try next model

        print(f"Gemini request failed after retries: {last_error}")
        return "I had trouble analyzing that image. Please try again in a moment."

