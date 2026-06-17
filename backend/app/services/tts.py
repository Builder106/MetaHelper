import edge_tts
import asyncio
import io
import re

_HTTP_LINK = re.compile(r"\[([^\]]+)\]\(https?://[^)]*\)")
_FENCE = re.compile(r"```[^\n]*")
_HEADING = re.compile(r"(?m)^\s{0,3}#{1,6}\s+")
_BULLET = re.compile(r"(?m)^\s*[-*+]\s+")


def strip_markdown_for_speech(text: str) -> str:
    """Strip Markdown formatting that text-to-speech reads awkwardly (backticks,
    code fences, headings, bullet markers, link URLs, table pipes) while keeping
    the spoken words. Conservative on purpose: leaves '**'/'__' and inline
    symbols alone so code content (x**2, __init__, a | b) isn't mangled, and
    keeps numbered lists ('1.' reads naturally as 'one')."""
    if not text:
        return text
    text = _HTTP_LINK.sub(r"\1", text)   # [label](http...) -> label
    text = _FENCE.sub("", text)          # ``` and ```lang code fences
    text = text.replace("`", "")         # inline backticks
    text = _HEADING.sub("", text)        # ## headings
    text = _BULLET.sub("", text)         # -, *, + bullet markers
    text = text.replace("|", " ")        # table pipes
    text = re.sub(r"[ \t]{2,}", " ", text)
    return text.strip()


class TTSService:
    def __init__(self, voice: str = "en-US-GuyNeural"):
        self.voice = voice

    async def text_to_speech(self, text: str) -> bytes:
        text = strip_markdown_for_speech(text)
        if not text.strip():
            print("TTS Warning: Received empty text, returning empty audio.")
            return b""
            
        print(f"Synthesizing speech for {len(text)} characters...")
        try:
            communicate = edge_tts.Communicate(text, self.voice)
            audio_data = b""
            async for chunk in communicate.stream():
                if chunk["type"] == "audio":
                    audio_data += chunk["data"]
            
            if not audio_data:
                raise Exception("No audio data received from TTS stream")
                
            return audio_data
        except Exception as e:
            print(f"TTS Error with {self.voice}: {str(e)}")
            # Fallback to a different voice if the primary fails
            fallback_voice = "en-US-AndrewNeural" if self.voice != "en-US-AndrewNeural" else "en-GB-SoniaNeural"
            print(f"Attempting fallback to {fallback_voice}...")
            try:
                communicate = edge_tts.Communicate(text, fallback_voice)
                audio_data = b""
                async for chunk in communicate.stream():
                    if chunk["type"] == "audio":
                        audio_data += chunk["data"]
                return audio_data
            except Exception as e2:
                print(f"Final TTS Failure: {str(e2)}")
                raise e2
