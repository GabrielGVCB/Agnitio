import io
import pytest
from PIL import Image, ImageDraw


@pytest.fixture
def sample_face_image_bytes() -> bytes:
    """Creates a small 120x120 synthetic portrait-like fixture image."""
    img = Image.new("RGB", (120, 120), color=(240, 220, 200))
    draw = ImageDraw.Draw(img)
    # Draw simple facial feature placeholders
    draw.ellipse((40, 35, 55, 50), fill=(50, 50, 50))  # left eye
    draw.ellipse((65, 35, 80, 50), fill=(50, 50, 50))  # right eye
    draw.line((60, 50, 60, 70), fill=(100, 50, 50), width=3)  # nose
    draw.arc((45, 75, 75, 95), start=0, end=180, fill=(150, 50, 50), width=4)  # mouth

    buf = io.BytesIO()
    img.save(buf, format="JPEG")
    buf.seek(0)
    return buf.read()


@pytest.fixture
def empty_image_bytes() -> bytes:
    """Creates an empty 2x2 image."""
    img = Image.new("RGB", (2, 2), color=(0, 0, 0))
    buf = io.BytesIO()
    img.save(buf, format="JPEG")
    buf.seek(0)
    return buf.read()
