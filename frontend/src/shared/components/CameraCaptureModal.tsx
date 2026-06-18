import { CameraOutlined, ReloadOutlined } from "@ant-design/icons";
import { Alert, Button, Modal, Space, Typography } from "antd";
import { useCallback, useEffect, useRef, useState } from "react";

type Props = {
  open: boolean;
  title: string;
  confirmText: string;
  loading?: boolean;
  onCancel: () => void;
  onCapture: (file: File) => void;
};

export default function CameraCaptureModal({ open, title, confirmText, loading, onCancel, onCapture }: Props) {
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const [ready, setReady] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const stopCamera = useCallback(() => {
    streamRef.current?.getTracks().forEach((track) => track.stop());
    streamRef.current = null;
    setReady(false);
  }, []);

  const startCamera = useCallback(async () => {
    setError(null);
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: "user", width: { ideal: 1280 }, height: { ideal: 720 } },
        audio: false
      });
      stopCamera();
      streamRef.current = stream;
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
        await videoRef.current.play();
      }
      setReady(true);
    } catch {
      setReady(false);
      setError("Camera permission is required");
    }
  }, [stopCamera]);

  useEffect(() => {
    if (open) {
      void startCamera();
    } else {
      stopCamera();
    }
    return stopCamera;
  }, [open, startCamera, stopCamera]);

  const capture = useCallback(async () => {
    const video = videoRef.current;
    const canvas = canvasRef.current;
    if (!video || !canvas || video.videoWidth === 0 || video.videoHeight === 0) {
      setError("Camera is not ready");
      return;
    }

    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    const context = canvas.getContext("2d");
    if (!context) {
      setError("Camera capture failed");
      return;
    }

    context.drawImage(video, 0, 0, canvas.width, canvas.height);
    const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, "image/jpeg", 0.9));
    if (!blob) {
      setError("Camera capture failed");
      return;
    }
    onCapture(new File([blob], `camera-${Date.now()}.jpg`, { type: "image/jpeg" }));
  }, [onCapture]);

  return (
    <Modal
      open={open}
      title={title}
      onCancel={onCancel}
      destroyOnClose
      footer={[
        <Button key="restart" icon={<ReloadOutlined />} onClick={() => void startCamera()}>
          Restart
        </Button>,
        <Button key="cancel" onClick={onCancel}>
          Cancel
        </Button>,
        <Button key="capture" type="primary" icon={<CameraOutlined />} disabled={!ready} loading={loading} onClick={() => void capture()}>
          {confirmText}
        </Button>
      ]}
    >
      <Space direction="vertical" size={12} className="full-width">
        {error ? <Alert type="error" message={error} showIcon /> : null}
        <div className="camera-preview employee-camera-preview">
          <video ref={videoRef} className="camera-video" playsInline muted />
          <canvas ref={canvasRef} className="camera-canvas" />
          {!ready ? (
            <div className="camera-placeholder">
              <CameraOutlined />
              <Typography.Text>Starting camera</Typography.Text>
            </div>
          ) : null}
        </div>
      </Space>
    </Modal>
  );
}
