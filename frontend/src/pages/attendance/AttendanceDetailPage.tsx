import { EditOutlined } from "@ant-design/icons";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Alert, Button, Card, Descriptions, Empty, Space, Spin, Tag, Typography, message } from "antd";
import dayjs from "dayjs";
import { useState } from "react";
import { useParams } from "react-router-dom";
import {
  AttendanceResponse,
  AttendanceStatus,
  attendanceApi,
  formatMinutes
} from "../../features/attendance/api/attendanceApi";
import { branchApi } from "../../features/branch/api/branchApi";
import { employeeApi } from "../../features/employee/api/employeeApi";
import AttendanceUpdateModal from "./AttendanceUpdateModal";

function formatDateTime(value?: string | null) {
  return value ? dayjs(value).format("YYYY-MM-DD HH:mm") : "-";
}

function statusTag(status: AttendanceStatus) {
  const color = status === "LATE" ? "red" : status === "PRESENT" ? "green" : "default";
  return <Tag color={color}>{status}</Tag>;
}

export default function AttendanceDetailPage() {
  const { id } = useParams();
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<AttendanceResponse>();

  const attendanceQuery = useQuery({
    queryKey: ["attendance", "detail", id],
    queryFn: () => attendanceApi.getById(id!),
    enabled: Boolean(id)
  });

  const employeeQuery = useQuery({
    queryKey: ["employees", "detail", attendanceQuery.data?.employeeId],
    queryFn: () => employeeApi.getEmployee(attendanceQuery.data!.employeeId),
    enabled: Boolean(attendanceQuery.data?.employeeId)
  });

  const branchQuery = useQuery({
    queryKey: ["branches", "detail", attendanceQuery.data?.branchId],
    queryFn: () => branchApi.getBranch(attendanceQuery.data!.branchId),
    enabled: Boolean(attendanceQuery.data?.branchId)
  });

  const updateMutation = useMutation({
    mutationFn: ({ attendanceId, values }: { attendanceId: string; values: Parameters<typeof attendanceApi.updateAttendance>[1] }) =>
      attendanceApi.updateAttendance(attendanceId, values),
    onSuccess: () => {
      message.success("Attendance updated");
      setEditing(undefined);
      queryClient.invalidateQueries({ queryKey: ["attendance"] });
    },
    onError: () => message.error("Failed to update attendance")
  });

  const attendance = attendanceQuery.data;
  const employeeName = employeeQuery.data
    ? `${employeeQuery.data.firstName} ${employeeQuery.data.lastName} (${employeeQuery.data.employeeCode})`
    : attendance?.employeeId;

  if (attendanceQuery.isLoading) {
    return (
      <div className="centered-state">
        <Spin />
      </div>
    );
  }

  return (
    <Space direction="vertical" size={16} className="page-stack">
      <div className="page-toolbar">
        <div>
          <Typography.Title level={3}>Attendance Detail</Typography.Title>
          <Typography.Text type="secondary">View and update attendance session information.</Typography.Text>
        </div>
        {attendance ? (
          <Button type="primary" icon={<EditOutlined />} onClick={() => setEditing(attendance)}>
            Edit
          </Button>
        ) : null}
      </div>

      {attendanceQuery.isError ? <Alert type="error" message="Failed to load attendance" showIcon /> : null}

      {attendance ? (
        <Card>
          <Descriptions
            bordered
            column={{ xs: 1, sm: 1, md: 2 }}
            title={
              <Space>
                <span>{attendance.date}</span>
                {statusTag(attendance.status)}
              </Space>
            }
          >
            <Descriptions.Item label="Employee">{employeeName}</Descriptions.Item>
            <Descriptions.Item label="Branch">{branchQuery.data?.name ?? attendance.branchId}</Descriptions.Item>
            <Descriptions.Item label="Date">{attendance.date}</Descriptions.Item>
            <Descriptions.Item label="Check-in">{formatDateTime(attendance.checkInTime)}</Descriptions.Item>
            <Descriptions.Item label="Check-out">{formatDateTime(attendance.checkOutTime)}</Descriptions.Item>
            <Descriptions.Item label="Late minutes">
              <Tag color={attendance.lateMinutes > 0 ? "red" : "default"}>{attendance.lateMinutes} min</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="Work minutes">{formatMinutes(attendance.workMinutes)}</Descriptions.Item>
            <Descriptions.Item label="Method">{attendance.method}</Descriptions.Item>
            <Descriptions.Item label="Session type">{attendance.sessionType}</Descriptions.Item>
            <Descriptions.Item label="Source device">{attendance.sourceDeviceId ?? "-"}</Descriptions.Item>
            <Descriptions.Item label="Note">{attendance.note ?? "-"}</Descriptions.Item>
          </Descriptions>
        </Card>
      ) : (
        <Empty description="Attendance not found" />
      )}

      <AttendanceUpdateModal
        open={Boolean(editing)}
        attendance={editing}
        loading={updateMutation.isPending}
        onCancel={() => setEditing(undefined)}
        onSubmit={(values) => editing && updateMutation.mutate({ attendanceId: editing.id, values })}
      />
    </Space>
  );
}
