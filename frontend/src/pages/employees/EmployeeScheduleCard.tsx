import { SaveOutlined } from "@ant-design/icons";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Alert, Button, Card, Empty, Input, InputNumber, Space, Switch, Table, TimePicker, Typography, message } from "antd";
import type { ColumnsType } from "antd/es/table";
import dayjs, { Dayjs } from "dayjs";
import { useEffect, useMemo, useState } from "react";
import {
  EmployeeScheduleResponse,
  employeeScheduleApi
} from "../../features/employee/api/employeeScheduleApi";

type ScheduleRow = {
  dayOfWeek: number;
  dayName: string;
  startTime: string;
  endTime: string;
  lateThresholdMin: number;
  isWorkday: boolean;
  note?: string;
};

const dayNames = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"];

function defaultRows(): ScheduleRow[] {
  return dayNames.map((dayName, index) => ({
    dayOfWeek: index + 1,
    dayName,
    startTime: "09:00:00",
    endTime: "18:00:00",
    lateThresholdMin: 15,
    isWorkday: index < 5,
    note: ""
  }));
}

function toRows(schedules: EmployeeScheduleResponse[]): ScheduleRow[] {
  const byDay = new Map(schedules.map((schedule) => [schedule.dayOfWeek, schedule]));
  return defaultRows().map((row) => {
    const schedule = byDay.get(row.dayOfWeek);
    return schedule
      ? {
          ...row,
          startTime: schedule.startTime,
          endTime: schedule.endTime,
          lateThresholdMin: schedule.lateThresholdMin,
          isWorkday: schedule.isWorkday,
          note: schedule.note ?? ""
        }
      : row;
  });
}

function timeValue(value: string): Dayjs {
  return dayjs(`2026-01-01T${value.length === 5 ? `${value}:00` : value}`);
}

function toTimeString(value: Dayjs | null): string {
  return value ? value.format("HH:mm:ss") : "09:00:00";
}

type Props = {
  employeeId?: string;
  title?: string;
};

export default function EmployeeScheduleCard({ employeeId, title = "Work Schedule" }: Props) {
  const queryClient = useQueryClient();
  const [rows, setRows] = useState<ScheduleRow[]>(defaultRows());

  const scheduleQuery = useQuery({
    queryKey: ["employees", "schedule", employeeId],
    queryFn: () => employeeScheduleApi.getSchedule(employeeId!),
    enabled: Boolean(employeeId)
  });

  useEffect(() => {
    if (scheduleQuery.data) {
      setRows(toRows(scheduleQuery.data));
    }
  }, [scheduleQuery.data]);

  const hasPersonalSchedule = Boolean(scheduleQuery.data?.length);

  const updateMutation = useMutation({
    mutationFn: () =>
      employeeScheduleApi.updateSchedule(employeeId!, {
        schedules: rows.map((row) => ({
          dayOfWeek: row.dayOfWeek,
          startTime: row.startTime,
          endTime: row.endTime,
          lateThresholdMin: row.lateThresholdMin,
          isWorkday: row.isWorkday,
          note: row.note?.trim() || undefined
        }))
      }),
    onSuccess: (data) => {
      message.success("Employee schedule saved");
      setRows(toRows(data));
      queryClient.invalidateQueries({ queryKey: ["employees", "schedule", employeeId] });
    },
    onError: () => message.error("Failed to save employee schedule")
  });

  const setRow = (dayOfWeek: number, patch: Partial<ScheduleRow>) => {
    setRows((current) => current.map((row) => (row.dayOfWeek === dayOfWeek ? { ...row, ...patch } : row)));
  };

  const columns = useMemo<ColumnsType<ScheduleRow>>(
    () => [
      {
        title: "Day",
        dataIndex: "dayName",
        key: "dayName",
        width: 140
      },
      {
        title: "Workday",
        dataIndex: "isWorkday",
        key: "isWorkday",
        width: 120,
        render: (value: boolean, row) => (
          <Switch checked={value} onChange={(checked) => setRow(row.dayOfWeek, { isWorkday: checked })} />
        )
      },
      {
        title: "Start",
        dataIndex: "startTime",
        key: "startTime",
        width: 150,
        render: (value: string, row) => (
          <TimePicker
            format="HH:mm"
            value={timeValue(value)}
            onChange={(next) => setRow(row.dayOfWeek, { startTime: toTimeString(next) })}
          />
        )
      },
      {
        title: "End",
        dataIndex: "endTime",
        key: "endTime",
        width: 150,
        render: (value: string, row) => (
          <TimePicker
            format="HH:mm"
            value={timeValue(value)}
            onChange={(next) => setRow(row.dayOfWeek, { endTime: toTimeString(next) })}
          />
        )
      },
      {
        title: "Late threshold",
        dataIndex: "lateThresholdMin",
        key: "lateThresholdMin",
        width: 160,
        render: (value: number, row) => (
          <InputNumber
            min={0}
            addonAfter="min"
            value={value}
            onChange={(next) => setRow(row.dayOfWeek, { lateThresholdMin: next ?? 0 })}
          />
        )
      },
      {
        title: "Note",
        dataIndex: "note",
        key: "note",
        render: (value: string, row) => (
          <Input
            placeholder="Optional"
            value={value}
            onChange={(event) => setRow(row.dayOfWeek, { note: event.target.value })}
          />
        )
      }
    ],
    []
  );

  if (!employeeId) {
    return (
      <Card title={title}>
        <Empty description="Create and save the employee first, then configure a personal work schedule." />
      </Card>
    );
  }

  return (
    <Card
      title={title}
      extra={
        <Button
          type="primary"
          icon={<SaveOutlined />}
          loading={updateMutation.isPending}
          onClick={() => updateMutation.mutate()}
        >
          Save Schedule
        </Button>
      }
      loading={scheduleQuery.isLoading}
    >
      <Space direction="vertical" size={12} className="full-width">
        {scheduleQuery.isError ? <Alert type="error" showIcon message="Failed to load employee schedule" /> : null}
        <Alert
          type={hasPersonalSchedule ? "success" : "info"}
          showIcon
          message={
            hasPersonalSchedule
              ? "This employee has a personal schedule. Attendance late minutes will use this schedule first."
              : "No personal schedule is saved yet. Attendance will use the branch default schedule until you save this table."
          }
        />
        <Typography.Text type="secondary">
          Personal schedule overrides branch schedule only for this employee.
        </Typography.Text>
        <Table
          rowKey="dayOfWeek"
          columns={columns}
          dataSource={rows}
          pagination={false}
          scroll={{ x: 900 }}
        />
      </Space>
    </Card>
  );
}
