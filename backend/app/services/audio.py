from pydub import AudioSegment
import io

class AudioProcessor:
    def scale_amplitude(self, audio_bytes: bytes, multiplier: float = 0.1) -> bytes:
        # Load audio from bytes
        audio = AudioSegment.from_file(io.BytesIO(audio_bytes))
        
        # Scale amplitude
        # multiplier 0.1 means 10% of original volume
        # pydub uses decibels for gain, but we can also use multiplier
        # For a multiplier k, the dB change is 20 * log10(k)
        scaled_audio = audio + (20 * 0.1) # This is wrong logic for multiplier
        
        # Correct way to scale by multiplier:
        # Note: audio * multiplier works in pydub if multiplier is a factor
        scaled_audio = audio - (audio.max_dBFS - (audio.max_dBFS + (20 * 0.1))) # Still messy
        
        # The simplest way to reduce volume by X% is:
        # scaled_audio = audio + dB_change
        import math
        db_change = 20 * math.log10(multiplier) if multiplier > 0 else -100
        scaled_audio = audio + db_change
        
        # Export back to bytes
        buffer = io.BytesIO()
        scaled_audio.export(buffer, format="mp3")
        return buffer.getvalue()

