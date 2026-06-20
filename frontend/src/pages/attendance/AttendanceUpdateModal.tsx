import { DatePicker, Form, Input, Modal, Select } from "antd";
import dayjs, { Dayjs } from "dayjs";
import { useEffect } from "react";
import {
  AttendanceResponse,
  AttendanceStatus,
  UpdateAttendanceRequest
} from "../../features/attendance/api/attendanceApi";
import { useLookupOptions } from "../../shared/hooks/useLookups";

type FormValues = {
  checkInTime?: Dayjs;
  checkOutTime?: Dayjs;
  status?: AttendanceStatus;
  note?: string;
};

type AttendanceUpdateModalProps = {
  open: boolean;
  attendance?: AttendanceResponse;
  loading?: boolean;
  onCancel: () => void;
  onSubmit: (values: UpdateAttendanceRequest) => void;
};

function toDayjs(value?: string | null) {
  return value ? dayjs(value) : undefined;
}

function toInstant(value?: Dayjs) {
  return value ? value.toISOString() : undefined;
}

export default function AttendanceUpdateModal({
  open,
  attendance,
  loading,
  onCancel,
  onSubmit
}: AttendanceUpdateModalProps) {
  const [form] = Form.useForm<FormValues>();
  const statusOptions = useLookupOptions("attendanceStatuses");

  useEffect(() => {
    if (attendance && open) {
      form.setFieldsValue({
        checkInTime: toDayjs(attendance.checkInTime),
        checkOutTime: toDayjs(attendance.checkOutTime),
        status: attendance.status,
        note: attendance.note ?? undefined
      });
    }
  }, [attendance, form, open]);

  return (
    <Modal
      title="Update Attendance"
      open={open}
      onCancel={onCancel}
      onOk={() => form.submit()}
      confirmLoading={loading}
      destroyOnClose
    >
      <Form<FormValues>
        form={form}
        layout="vertical"
        onFinish={(values) =>
          onSubmit({
            checkInTime: toInstant(values.checkInTime),
            checkOutTime: toInstant(values.checkOutTime),
            status: values.status,
            note: values.note
          })
        }
      >
        <Form.Item name="checkInTime" label="Check-in time">
          <DatePicker showTime className="full-width" />
        </Form.Item>

        <Form.Item name="checkOutTime" label="Check-out time">
          <DatePicker showTime className="full-width" />
        </Form.Item>

      <Form.Item name="status" label="Status">
        <Select options={statusOptions.options} loading={statusOptions.isLoading} />
      </Form.Item>

        <Form.Item name="note" label="Note">
          <Input.TextArea rows={3} />
        </Form.Item>
      </Form>
    </Modal>
  );
}
