import google.generativeai as genai
import PIL.Image
import io

class VisionService:
    def __init__(self, api_key: str):
        genai.configure(api_key=api_key)
        self.model = genai.GenerativeModel('gemini-1.5-flash')

    def get_description(self, image_bytes: bytes) -> str:
        image = PIL.Image.open(io.BytesIO(image_bytes))
        prompt = "Describe what is in this image briefly and concisely for someone wearing smart glasses."
        response = self.model.generate_content([prompt, image])
        return response.text

