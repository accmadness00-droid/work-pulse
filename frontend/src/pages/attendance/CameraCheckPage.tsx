import { CameraOutlined, EnvironmentOutlined, LoginOutlined, LogoutOutlined, ReloadOutlined } from "@ant-design/icons";
import { useMutation, useQuery } from "@tanstack/react-query";
import { Alert, Button, Card, Select, Space, Statistic, Tag, Typography, message } from "antd";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { attendanceApi, type CameraAttendanceRequest, type CameraAttendanceResponse } from "../../features/attendance/api/attendanceApi";
import { branchApi } from "../../features/branch/api/branchApi";
import { useAuth } from "../../shared/auth/useAuth";

type LocationState = {
  latitude: number;
  longitude: number;
  accuracyMeters?: number | null;
};

function cameraErrorMessage(error: unknown) {
  if (!window.isSecureContext) {
    return "Camera requires a secure HTTPS connection";
  }
  if (error instanceof DOMException) {
    if (error.name === "NotFoundError") {
      return "No camera was found on this device";
    }
    if (error.name === "NotReadableError") {
      return "Camera is already in use by another application";
    }
  }
  return "Camera permission is required";
}

function locationErrorMessage(error: GeolocationPositionError) {
  if (!window.isSecureContext) {
    return "Location requires a secure HTTPS connection";
  }
  if (error.code === error.TIMEOUT) {
    return "Location request timed out. Please try again";
  }
  if (error.code === error.POSITION_UNAVAILABLE) {
    return "Current location is unavailable";
  }
  return "Location permission is required";
}

function errorMessage(error: unknown, fallback: string) {
  const maybe = error as { response?: { data?: { message?: string } } };
  return maybe.response?.data?.message ?? fallback;
}

export default function CameraCheckPage() {
  const { user } = useAuth();
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const cameraRequestRef = useRef(0);
  const locationRequestRef = useRef(0);
  const [cameraReady, setCameraReady] = useState(false);
  const [cameraError, setCameraError] = useState<string | null>(null);
  const [location, setLocation] = useState<LocationState | null>(null);
  const [locationError, setLocationError] = useState<string | null>(null);
  const [branchId, setBranchId] = useState<string | undefined>(user?.branchId ?? undefined);
  const [lastResult, setLastResult] = useState<CameraAttendanceResponse | null>(null);

  useEffect(() => {
    setBranchId(user?.branchId ?? undefined);
  }, [user?.branchId]);

  const branchesQuery = useQuery({
    queryKey: ["branches", "camera-check", user?.companyId],
    queryFn: () => branchApi.listBranches(user!.companyId!),
    enabled: Boolean(user?.companyId)
  });

  const selectedBranch = useMemo(
    () => (branchesQuery.data ?? []).find((branch) => branch.id === branchId),
    [branchesQuery.data, branchId]
  );

  const startCamera = useCallback(async () => {
    const requestId = ++cameraRequestRef.current;
    setCameraError(null);
    setCameraReady(false);
    streamRef.current?.getTracks().forEach((track) => track.stop());
    streamRef.current = null;

    if (!navigator.mediaDevices?.getUserMedia) {
      setCameraError(window.isSecureContext ? "Camera is not supported by this browser" : "Camera requires a secure HTTPS connection");
      return;
    }

    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: "user", width: { ideal: 1280 }, height: { ideal: 720 } },
        audio: false
      });

      if (requestId !== cameraRequestRef.current) {
        stream.getTracks().forEach((track) => track.stop());
        return;
      }

      streamRef.current = stream;
      const video = videoRef.current;
      if (!video) {
        stream.getTracks().forEach((track) => track.stop());
        return;
      }

      video.srcObject = stream;
      await video.play();

      if (requestId !== cameraRequestRef.current) {
        stream.getTracks().forEach((track) => track.stop());
        return;
      }

      setCameraError(null);
      setCameraReady(true);
    } catch (error) {
      if (requestId !== cameraRequestRef.current) {
        return;
      }
      streamRef.current?.getTracks().forEach((track) => track.stop());
      streamRef.current = null;
      setCameraReady(false);
      setCameraError(cameraErrorMessage(error));
    }
  }, []);

  const refreshLocation = useCallback(() => {
    const requestId = ++locationRequestRef.current;
    setLocationError(null);
    if (!navigator.geolocation) {
      setLocationError("Geolocation is not available");
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (position) => {
        if (requestId !== locationRequestRef.current) {
          return;
        }
        setLocation({
          latitude: position.coords.latitude,
          longitude: position.coords.longitude,
          accuracyMeters: position.coords.accuracy
        });
        setLocationError(null);
      },
      (error) => {
        if (requestId !== locationRequestRef.current) {
          return;
        }
        setLocation(null);
        setLocationError(locationErrorMessage(error));
      },
      { enableHighAccuracy: true, timeout: 12000, maximumAge: 15000 }
    );
  }, []);

  useEffect(() => {
    void startCamera();
    refreshLocation();
    return () => {
      cameraRequestRef.current += 1;
      locationRequestRef.current += 1;
      streamRef.current?.getTracks().forEach((track) => track.stop());
      streamRef.current = null;
    };
  }, [refreshLocation, startCamera]);

  useEffect(() => {
    if (!navigator.permissions?.query) {
      return;
    }

    const removeListeners: Array<() => void> = [];
    let disposed = false;

    const watchPermission = async (name: "camera" | "geolocation", onGranted: () => void) => {
      try {
        const status = await navigator.permissions.query({ name: name as PermissionName });
        if (disposed) {
          return;
        }
        const handleChange = () => {
          if (status.state === "granted") {
            onGranted();
          }
        };
        status.addEventListener("change", handleChange);
        removeListeners.push(() => status.removeEventListener("change", handleChange));
      } catch {
        // Some browsers expose Permissions API without supporting camera queries.
      }
    };

    void watchPermission("camera", () => void startCamera());
    void watchPermission("geolocation", refreshLocation);

    return () => {
      disposed = true;
      removeListeners.forEach((removeListener) => removeListener());
    };
  }, [refreshLocation, startCamera]);

  const capturePhoto = useCallback(() => {
    const video = videoRef.current;
    const canvas = canvasRef.current;
    if (!video || !canvas || video.videoWidth === 0 || video.videoHeight === 0) {
      throw new Error("Camera is not ready");
    }
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    const context = canvas.getContext("2d");
    if (!context) {
      throw new Error("Camera capture failed");
    }
    context.drawImage(video, 0, 0, canvas.width, canvas.height);
    return canvas.toDataURL("image/jpeg", 0.88);
  }, []);

  const buildRequest = useCallback((): CameraAttendanceRequest => {
    if (!branchId) {
      throw new Error("Branch is required");
    }
    if (!location) {
      throw new Error("Location is required");
    }
    return {
      branchId,
      latitude: location.latitude,
      longitude: location.longitude,
      accuracyMeters: location.accuracyMeters,
      photoBase64: capturePhoto()
    };
  }, [branchId, capturePhoto, location]);

  const checkInMutation = useMutation({
    mutationFn: () => attendanceApi.cameraCheckIn(buildRequest()),
    onSuccess: (result) => {
      setLastResult(result);
      message.success("Camera check-in saved");
    },
    onError: (error) => message.error(errorMessage(error, "Camera check-in failed"))
  });

  const checkOutMutation = useMutation({
    mutationFn: () => attendanceApi.cameraCheckOut(buildRequest()),
    onSuccess: (result) => {
      setLastResult(result);
      message.success("Camera check-out saved");
    },
    onError: (error) => message.error(errorMessage(error, "Camera check-out failed"))
  });

  const canSubmit = cameraReady && Boolean(location) && Boolean(branchId);
  const accuracy = location?.accuracyMeters == null ? null : Math.round(location.accuracyMeters);

  return (
    <Space direction="vertical" size={16} className="page-stack">
      <div className="page-toolbar">
        <div>
          <Typography.Title level={3}>Camera Check</Typography.Title>
          <Typography.Text type="secondary">Verify face and branch geofence before attendance is recorded.</Typography.Text>
        </div>
        <Button
          icon={<ReloadOutlined />}
          onClick={() => {
            void startCamera();
            refreshLocation();
          }}
        >
          Retry Permissions
        </Button>
      </div>

      {cameraError ? <Alert type="error" message={cameraError} showIcon /> : null}
      {locationError ? <Alert type="error" message={locationError} showIcon /> : null}
      {!user?.companyId ? <Alert type="warning" message="Current user is not linked to a company" showIcon /> : null}

      <div className="camera-check-grid">
        <Card className="camera-preview-card">
          <div className="camera-preview">
            <video ref={videoRef} className="camera-video" playsInline muted />
            <canvas ref={canvasRef} className="camera-canvas" />
            {!cameraReady ? (
              <div className="camera-placeholder">
                <CameraOutlined />
                <span>Starting camera</span>
              </div>
            ) : null}
          </div>
          <Space className="camera-actions" wrap>
            <Button icon={<CameraOutlined />} onClick={() => void startCamera()}>
              Restart Camera
            </Button>
            <Button
              type="primary"
              icon={<LoginOutlined />}
              disabled={!canSubmit}
              loading={checkInMutation.isPending}
              onClick={() => checkInMutation.mutate()}
            >
              Check In
            </Button>
            <Button
              icon={<LogoutOutlined />}
              disabled={!canSubmit}
              loading={checkOutMutation.isPending}
              onClick={() => checkOutMutation.mutate()}
            >
              Check Out
            </Button>
          </Space>
        </Card>

        <Space direction="vertical" size={16} className="camera-side">
          <Card>
            <Space direction="vertical" size={12} className="full-width">
              <Typography.Title level={5}>Branch</Typography.Title>
              <Select
                className="full-width"
                placeholder="Select branch"
                value={branchId}
                loading={branchesQuery.isLoading}
                options={(branchesQuery.data ?? []).map((branch) => ({ value: branch.id, label: branch.name }))}
                onChange={setBranchId}
              />
              {selectedBranch ? (
                <Typography.Text type="secondary">
                  {selectedBranch.address ?? "No address"} · radius {selectedBranch.geofenceRadiusMeters ?? selectedBranch.radiusMeters ?? 0}m
                </Typography.Text>
              ) : null}
            </Space>
          </Card>

          <Card>
            <Space direction="vertical" size={12} className="full-width">
              <Typography.Title level={5}>Verification</Typography.Title>
              <Space wrap>
                <Tag color={cameraReady ? "green" : "default"} icon={<CameraOutlined />}>
                  Camera
                </Tag>
                <Tag color={location ? "green" : "default"} icon={<EnvironmentOutlined />}>
                  Location
                </Tag>
                {accuracy == null ? <Tag>Accuracy pending</Tag> : <Tag color={accuracy <= 100 ? "green" : "orange"}>{accuracy}m accuracy</Tag>}
              </Space>
              {location ? (
                <Typography.Text type="secondary">
                  {location.latitude.toFixed(6)}, {location.longitude.toFixed(6)}
                </Typography.Text>
              ) : null}
            </Space>
          </Card>

          {lastResult ? (
            <Card>
              <Space direction="vertical" size={12} className="full-width">
                <Typography.Title level={5}>Last Result</Typography.Title>
                <Space wrap>
                  <Tag color="blue">{lastResult.action}</Tag>
                  <Tag color={lastResult.locationVerified ? "green" : "red"}>Location</Tag>
                  <Tag color={lastResult.faceVerified ? "green" : "red"}>Face</Tag>
                </Space>
                <Statistic title="Face distance" value={lastResult.faceDistance} precision={4} />
                {lastResult.workMinutes != null ? <Statistic title="Work minutes" value={lastResult.workMinutes} /> : null}
              </Space>
            </Card>
          ) : null}
        </Space>
      </div>
    </Space>
  );
}
