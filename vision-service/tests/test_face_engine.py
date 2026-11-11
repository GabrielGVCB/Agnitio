import numpy as np
from src.face_engine import DeterministicFaceEngine, l2_normalize


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
