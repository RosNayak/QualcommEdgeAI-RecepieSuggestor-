#!/usr/bin/env python3
"""
WhisperX Local Server for Android App
Ultra-fast speech recognition for voice commands
"""

import os
import io
import json
import tempfile
from flask import Flask, request, jsonify
from flask_cors import CORS
import whisperx
import torch
import logging

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = Flask(__name__)
CORS(app)

class WhisperXProcessor:
    def __init__(self):
        self.device = "cuda" if torch.cuda.is_available() else "cpu"
        self.compute_type = "float16" if torch.cuda.is_available() else "int8"

        logger.info(f"Initializing WhisperX on {self.device} with {self.compute_type}")

        # Load model - using tiny for speed
        self.model = whisperx.load_model("tiny", self.device, compute_type=self.compute_type)

        # Load alignment model
        self.align_model, self.metadata = whisperx.load_align_model(
            language_code="en", device=self.device
        )

        logger.info("WhisperX models loaded successfully")

    def transcribe_audio(self, audio_file_path):
        """Transcribe audio file and return text"""
        try:
            # Load audio
            audio = whisperx.load_audio(audio_file_path)

            # Transcribe
            result = whisperx.transcribe(audio, self.model, batch_size=16)

            # Align whisper output
            result = whisperx.align(
                result["segments"],
                self.align_model,
                self.metadata,
                audio,
                self.device,
                return_char_alignments=False
            )

            # Extract text
            text = ""
            if "segments" in result:
                text = " ".join([segment["text"] for segment in result["segments"]])

            return text.strip()

        except Exception as e:
            logger.error(f"Transcription error: {str(e)}")
            return ""

# Initialize processor
processor = WhisperXProcessor()

@app.route('/health', methods=['GET'])
def health_check():
    """Health check endpoint"""
    return jsonify({"status": "healthy", "model": "whisperx-tiny"})

@app.route('/transcribe', methods=['POST'])
def transcribe():
    """Transcribe audio file"""
    try:
        if 'file' not in request.files:
            return jsonify({"error": "No audio file provided"}), 400

        audio_file = request.files['file']
        if audio_file.filename == '':
            return jsonify({"error": "No file selected"}), 400

        # Save uploaded file temporarily
        with tempfile.NamedTemporaryFile(delete=False, suffix='.wav') as temp_file:
            audio_file.save(temp_file.name)
            temp_path = temp_file.name

        try:
            # Transcribe audio
            transcription = processor.transcribe_audio(temp_path)

            logger.info(f"Transcribed: '{transcription}'")

            return jsonify({
                "text": transcription,
                "success": True
            })

        finally:
            # Clean up temp file
            if os.path.exists(temp_path):
                os.unlink(temp_path)

    except Exception as e:
        logger.error(f"Transcription endpoint error: {str(e)}")
        return jsonify({"error": str(e), "success": False}), 500

@app.route('/command', methods=['POST'])
def detect_command():
    """Optimized endpoint for command detection"""
    try:
        if 'file' not in request.files:
            return jsonify({"error": "No audio file provided"}), 400

        audio_file = request.files['file']

        # Save uploaded file temporarily
        with tempfile.NamedTemporaryFile(delete=False, suffix='.wav') as temp_file:
            audio_file.save(temp_file.name)
            temp_path = temp_file.name

        try:
            # Transcribe audio
            transcription = processor.transcribe_audio(temp_path)

            # Check for update command (case insensitive)
            is_update_command = "update" in transcription.lower()

            logger.info(f"Command detection - Text: '{transcription}' -> Update: {is_update_command}")

            return jsonify({
                "text": transcription,
                "is_update_command": is_update_command,
                "success": True
            })

        finally:
            # Clean up temp file
            if os.path.exists(temp_path):
                os.unlink(temp_path)

    except Exception as e:
        logger.error(f"Command detection error: {str(e)}")
        return jsonify({"error": str(e), "success": False}), 500

if __name__ == '__main__':
    print("Starting WhisperX Server...")
    print(f"Device: {processor.device}")
    print("Server running on http://localhost:5000")
    app.run(host='0.0.0.0', port=5000, debug=False)