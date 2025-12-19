import edge_tts
import asyncio
import io

class TTSService:
    def __init__(self, voice: str = "en-US-GuyNeural"):
        self.voice = voice

    async def text_to_speech(self, text: str) -> bytes:
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
