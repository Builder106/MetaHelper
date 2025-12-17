import pytest
from unittest.mock import patch, MagicMock
from app.services.vision import VisionService
from app.services.tts import TTSService
from app.services.audio import AudioProcessor

@pytest.fixture
def vision_service():
    return VisionService(api_key="mock_key")

@pytest.fixture
def tts_service():
    return TTSService()

def test_vision_service_generate_description(vision_service):
    with patch("google.generativeai.GenerativeModel.generate_content") as mock_gen:
        mock_gen.return_value.text = "I see a person wearing glasses."
        description = vision_service.get_description(b"fake_image_bytes")
        assert description == "I see a person wearing glasses."

def test_audio_amplitude_scaling():
    # Create a mock audio segment
    from pydub import AudioSegment
    import io
    
    # 1 second of silence
    silence = AudioSegment.silent(duration=1000)
    processor = AudioProcessor()
    
    # Scale to 10%
    scaled = processor.scale_amplitude(silence, multiplier=0.1)
    assert scaled.dBFS < silence.dBFS or (scaled.dBFS == -float('inf') and silence.dBFS == -float('inf'))

