import edge_tts
import asyncio
import io

class TTSService:
    def __init__(self, voice: str = "en-US-GuyNeural"):
        self.voice = voice

    async def text_to_speech(self, text: str) -> bytes:
        communicate = edge_tts.Communicate(text, self.voice)
        audio_data = b""
        async for chunk in communicate.stream():
            if chunk["type"] == "audio":
                audio_data += chunk["data"]
        return audio_data
