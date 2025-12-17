from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

def test_read_root():
    response = client.get("/")
    assert response.status_code == 200
    assert response.json() == {"message": "MetaHelper API is running"}

def test_process_image_endpoint_exists():
    # We just check if the endpoint is reachable (returns 422 if no file provided)
    response = client.post("/process-image")
    assert response.status_code == 422

