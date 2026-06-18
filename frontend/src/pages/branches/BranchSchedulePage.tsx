import { SaveOutlined } from "@ant-design/icons";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Alert, Button, Card, InputNumber, Space, Switch, Table, TimePicker, Typography, message } from "antd";
import type { ColumnsType } from "antd/es/table";
import dayjs, { Dayjs } from "dayjs";
import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { BranchScheduleResponse, branchApi } from "../../features/branch/api/branchApi";

type ScheduleRow = {
  dayOfWeek: number;
  dayName: string;
  isWorkday: boolean;
  startTime: string;
  endTime: string;
  lateThresholdMin: number;
};

const dayNames = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"];

function defaultRows(): ScheduleRow[] {
  return dayNames.map((dayName, index) => ({
    dayOfWeek: index + 1,
    dayName,
    isWorkday: index < 5,
    startTime: "09:00:00",
    endTime: "18:00:00",
    lateThresholdMin: 15
  }));
}

function toRows(schedules: BranchScheduleResponse[]): ScheduleRow[] {
  const byDay = new Map(schedules.map((item) => [item.dayOfWeek, item]));
  return defaultRows().map((row) => {
    const schedule = byDay.get(row.dayOfWeek);
    return schedule
      ? {
          ...row,
          isWorkday: schedule.isWorkday,
          startTime: schedule.startTime,
          endTime: schedule.endTime,
          lateThresholdMin: schedule.lateThresholdMin
        }
      : row;
  });
}

function asTime(value: string): Dayjs {
  return dayjs(`2000-01-01T${value}`);
}

function formatTime(value: Dayjs | null): string {
  return value ? value.format("HH:mm:ss") : "09:00:00";
}

export default function BranchSchedulePage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { id } = useParams();
  const [rows, setRows] = useState<ScheduleRow[]>(defaultRows());

  const scheduleQuery = useQuery({
    queryKey: ["branches", id, "schedule"],
    queryFn: () => branchApi.getSchedule(id!),
    enabled: Boolean(id)
  });

  useEffect(() => {
    if (scheduleQuery.data) {
      setRows(toRows(scheduleQuery.data));
    }
  }, [scheduleQuery.data]);

  const mutation = useMutation({
    mutationFn: () =>
      branchApi.updateSchedule(id!, {
        schedules: rows.map((row) => ({
          dayOfWeek: row.dayOfWeek,
          startTime: row.startTime,
          endTime: row.endTime,
          lateThresholdMin: row.lateThresholdMin,
          isWorkday: row.isWorkday
        }))
      }),
    onSuccess: () => {
      message.success("Branch schedule saved");
      queryClient.invalidateQueries({ queryKey: ["branches", id, "schedule"] });
      navigate("/branches");
    },
    onError: () => {
      message.error("Failed to save branch schedule");
    }
  });

  const updateRow = (dayOfWeek: number, patch: Partial<ScheduleRow>) => {
    setRows((current) => current.map((row) => (row.dayOfWeek === dayOfWeek ? { ...row, ...patch } : row)));
  };

  const columns: ColumnsType<ScheduleRow> = [
    {
      title: "Day",
      dataIndex: "dayName",
      key: "dayName"
    },
    {
      title: "Workday",
      dataIndex: "isWorkday",
      key: "isWorkday",
      render: (value: boolean, row) => (
        <Switch checked={value} onChange={(checked) => updateRow(row.dayOfWeek, { isWorkday: checked })} />
      )
    },
    {
      title: "Start time",
      dataIndex: "startTime",
      key: "startTime",
      render: (value: string, row) => (
        <TimePicker
          value={asTime(value)}
          format="HH:mm"
          onChange={(time) => updateRow(row.dayOfWeek, { startTime: formatTime(time) })}
        />
      )
    },
    {
      title: "End time",
      dataIndex: "endTime",
      key: "endTime",
      render: (value: string, row) => (
        <TimePicker
          value={asTime(value)}
          format="HH:mm"
          onChange={(time) => updateRow(row.dayOfWeek, { endTime: formatTime(time) })}
        />
      )
    },
    {
      title: "Late threshold",
      dataIndex: "lateThresholdMin",
      key: "lateThresholdMin",
      render: (value: number, row) => (
        <InputNumber
          min={0}
          value={value}
          addonAfter="min"
          onChange={(nextValue) => updateRow(row.dayOfWeek, { lateThresholdMin: Number(nextValue ?? 0) })}
        />
      )
    }
  ];

  return (
    <Space direction="vertical" size={16} className="page-stack">
      <div>
        <Typography.Title level={3}>Branch Schedule</Typography.Title>
        <Typography.Text type="secondary">Edit weekly workday, time, and late threshold settings.</Typography.Text>
      </div>

      {scheduleQuery.isError ? <Alert type="error" message="Failed to load branch schedule" showIcon /> : null}

      <Card>
        <Table
          rowKey="dayOfWeek"
          columns={columns}
          dataSource={rows}
          loading={scheduleQuery.isLoading}
          pagination={false}
          scroll={{ x: 720 }}
        />
        <Space className="form-actions">
          <Button type="primary" icon={<SaveOutlined />} loading={mutation.isPending} onClick={() => mutation.mutate()}>
            Save
          </Button>
          <Button onClick={() => navigate("/branches")}>Cancel</Button>
        </Space>
      </Card>
    </Space>
  );
}
