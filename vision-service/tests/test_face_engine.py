import numpy as np
import pytest

from src import face_engine as face_engine_module
from src.face_engine import DeterministicFaceEngine, create_face_engine, get_face_engine, l2_normalize


def test_l2_normalize():
    vec = np.array([3.0, 4.0], dtype=np.float32)
    normed = l2_normalize(vec)
    assert np.isclose(np.linalg.norm(normed), 1.0)


def test_deterministic_face_engine_detects_face(sample_face_image_bytes):
    engine = DeterministicFaceEngine(dimension=512)
    faces = engine.detect_and_extract_faces(sample_face_image_bytes)

    assert len(faces) == 1
    face = faces[0]
    assert len(face["embedding"]) == 512
    # Verify embedding is L2 normalized
    norm = np.linalg.norm(np.array(face["embedding"]))
    assert np.isclose(norm, 1.0, atol=1e-5)
    assert face["score"] > 0.9
    assert face["bbox"]["width"] > 0
    assert face["bbox"]["height"] > 0


def test_deterministic_face_engine_selfie_embedding(sample_face_image_bytes):
    engine = DeterministicFaceEngine(dimension=512)
    emb = engine.extract_selfie_embedding(sample_face_image_bytes)

    assert emb is not None
    assert len(emb) == 512
    assert np.isclose(np.linalg.norm(np.array(emb)), 1.0, atol=1e-5)


def test_deterministic_face_engine_empty_image(empty_image_bytes):
    engine = DeterministicFaceEngine(dimension=512)
    faces = engine.detect_and_extract_faces(empty_image_bytes)
    assert len(faces) == 0


class _BrokenInsightFace:
    def __init__(self, *args, **kwargs):
        raise RuntimeError("model not available")


def test_insightface_mode_fails_loudly_instead_of_falling_back(monkeypatch):
    monkeypatch.setattr(face_engine_module, "InsightFaceEngine", _BrokenInsightFace)

    with pytest.raises(RuntimeError, match="model not available"):
        create_face_engine("insightface")


def test_auto_mode_falls_back_to_deterministic_when_insightface_unavailable(monkeypatch):
    monkeypatch.setattr(face_engine_module, "InsightFaceEngine", _BrokenInsightFace)

    assert isinstance(create_face_engine("auto"), DeterministicFaceEngine)


def test_deterministic_mode():
    assert isinstance(create_face_engine("deterministic"), DeterministicFaceEngine)


def test_invalid_engine_type_is_rejected():
    with pytest.raises(ValueError, match="Invalid FACE_ENGINE_TYPE"):
        create_face_engine("magic")


def test_get_face_engine_uses_configured_type(monkeypatch):
    from src.config import settings

    monkeypatch.setattr(face_engine_module, "_face_engine_instance", None)
    monkeypatch.setattr(settings, "FACE_ENGINE_TYPE", "deterministic")

    assert isinstance(get_face_engine(), DeterministicFaceEngine)
