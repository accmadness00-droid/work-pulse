import {
  CameraOutlined,
  DeleteOutlined,
  EditOutlined,
  CloudUploadOutlined,
  PlusOutlined,
  PoweroffOutlined,
  UploadOutlined,
  UserOutlined
} from "@ant-design/icons";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  Alert,
  Avatar,
  Button,
  Card,
  Descriptions,
  Empty,
  Popconfirm,
  Space,
  Spin,
  Table,
  Tabs,
  Tag,
  Typography,
  Upload,
  message
} from "antd";
import type { ColumnsType } from "antd/es/table";
import { useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { branchApi } from "../../features/branch/api/branchApi";
import { EmployeeFaceProfileResponse, faceApi } from "../../features/face/api/faceApi";
import {
  EmployeeCredentialResponse,
  employeeCredentialApi
} from "../../features/employeeCredential/api/employeeCredentialApi";
import { employeeApi, employeePhotoUrl } from "../../features/employee/api/employeeApi";
import CameraCaptureModal from "../../shared/components/CameraCaptureModal";
import { useAccessibleCompanies } from "../../shared/hooks/useAccessibleCompanies";
import EmployeeScheduleCard from "./EmployeeScheduleCard";

function display(value?: string | number | null) {
  return value ?? "-";
}

export default function EmployeeDetailPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { id } = useParams();
  const [photoCameraOpen, setPhotoCameraOpen] = useState(false);
  const [faceCameraOpen, setFaceCameraOpen] = useState(false);
  const companiesQuery = useAccessibleCompanies();

  const employeeQuery = useQuery({
    queryKey: ["employees", "detail", id],
    queryFn: () => employeeApi.getEmployee(id!),
    enabled: Boolean(id)
  });

  const branchQuery = useQuery({
    queryKey: ["branches", "detail", employeeQuery.data?.branchId],
    queryFn: () => branchApi.getBranch(employeeQuery.data!.branchId),
    enabled: Boolean(employeeQuery.data?.branchId)
  });

  const credentialsQuery = useQuery({
    queryKey: ["credentials", { employeeId: id }],
    queryFn: () => employeeCredentialApi.listCredentials({ employeeId: id }),
    enabled: Boolean(id)
  });

  const faceProfilesQuery = useQuery({
    queryKey: ["face-profiles", id],
    queryFn: () => faceApi.listProfiles(id!),
    enabled: Boolean(id)
  });

  const credentialStatusMutation = useMutation({
    mutationFn: ({ credentialId, nextActive }: { credentialId: string; nextActive: boolean }) =>
      nextActive
        ? employeeCredentialApi.activateCredential(credentialId)
        : employeeCredentialApi.deactivateCredential(credentialId),
    onSuccess: (_, variables) => {
      message.success(variables.nextActive ? "Credential activated" : "Credential deactivated");
      queryClient.invalidateQueries({ queryKey: ["credentials"] });
    },
    onError: () => message.error("Failed to update credential")
  });

  const credentialDeleteMutation = useMutation({
    mutationFn: employeeCredentialApi.deleteCredential,
    onSuccess: () => {
      message.success("Credential deleted");
      queryClient.invalidateQueries({ queryKey: ["credentials"] });
    },
    onError: () => message.error("Failed to delete credential")
  });

  const photoMutation = useMutation({
    mutationFn: (file: File) => employeeApi.uploadPhoto(id!, file),
    onSuccess: (updatedEmployee) => {
      message.success("Employee photo uploaded");
      setPhotoCameraOpen(false);
      queryClient.setQueryData(["employees", "detail", id], updatedEmployee);
      queryClient.invalidateQueries({ queryKey: ["employees"] });
    },
    onError: () => message.error("Failed to upload employee photo")
  });

  const faceEnrollMutation = useMutation({
    mutationFn: (file: File) => faceApi.enroll(id!, file),
    onSuccess: () => {
      message.success("Face enrolled");
      setFaceCameraOpen(false);
      queryClient.invalidateQueries({ queryKey: ["face-profiles", id] });
      queryClient.invalidateQueries({ queryKey: ["employees", "detail", id] });
    },
    onError: (error: unknown) => {
      const maybe = error as { response?: { data?: { message?: string } } };
      message.error(maybe.response?.data?.message ?? "Failed to enroll face");
    }
  });

  const faceDeactivateMutation = useMutation({
    mutationFn: (profileId: string) => faceApi.deactivate(id!, profileId),
    onSuccess: () => {
      message.success("Face profile deactivated");
      queryClient.invalidateQueries({ queryKey: ["face-profiles", id] });
    },
    onError: () => message.error("Failed to deactivate face profile")
  });

  const hikvisionSyncMutation = useMutation({
    mutationFn: () => employeeApi.syncPhotoToHikvision(id!),
    onSuccess: (result) => {
      if (result.totalDevices === 0) {
        message.info("No active Hikvision devices found for this branch");
        return;
      }
      if (result.failureCount > 0) {
        message.warning(`Photo sent to ${result.successCount}/${result.totalDevices} Hikvision devices`);
        return;
      }
      message.success(`Photo sent to ${result.successCount} Hikvision devices`);
    },
    onError: (error: unknown) => {
      const maybe = error as { response?: { data?: { message?: string } } };
      message.error(maybe.response?.data?.message ?? "Failed to sync photo to Hikvision");
    }
  });

  const employee = employeeQuery.data;
  const photoSrc = employeePhotoUrl(employee?.photoUrl);

  const credentialColumns: ColumnsType<EmployeeCredentialResponse> = [
    {
      title: "Type",
      dataIndex: "credentialType",
      key: "credentialType"
    },
    {
      title: "External ID",
      dataIndex: "externalId",
      key: "externalId"
    },
    {
      title: "Status",
      dataIndex: "active",
      key: "active",
      render: (active: boolean) => <Tag color={active ? "green" : "default"}>{active ? "Active" : "Inactive"}</Tag>
    },
    {
      title: "Actions",
      key: "actions",
      width: 240,
      render: (_, record) => (
        <Space>
          <Popconfirm
            title={record.active ? "Deactivate credential?" : "Activate credential?"}
            okText={record.active ? "Deactivate" : "Activate"}
            okButtonProps={{ danger: record.active }}
            onConfirm={() => credentialStatusMutation.mutate({ credentialId: record.id, nextActive: !record.active })}
          >
            <Button icon={<PoweroffOutlined />} danger={record.active} loading={credentialStatusMutation.isPending}>
              {record.active ? "Deactivate" : "Activate"}
            </Button>
          </Popconfirm>
          <Popconfirm
            title="Delete credential?"
            okText="Delete"
            okButtonProps={{ danger: true }}
            onConfirm={() => credentialDeleteMutation.mutate(record.id)}
          >
            <Button danger icon={<DeleteOutlined />} loading={credentialDeleteMutation.isPending}>
              Delete
            </Button>
          </Popconfirm>
        </Space>
      )
    }
  ];

  const faceProfileColumns: ColumnsType<EmployeeFaceProfileResponse> = [
    {
      title: "Photo",
      dataIndex: "photoUrl",
      key: "photoUrl",
      width: 90,
      render: (photoUrl?: string | null) => <Avatar size={48} src={employeePhotoUrl(photoUrl)} icon={<UserOutlined />} />
    },
    {
      title: "Model",
      dataIndex: "modelName",
      key: "modelName"
    },
    {
      title: "Created",
      dataIndex: "createdAt",
      key: "createdAt",
      render: (createdAt?: string | null) => display(createdAt)
    },
    {
      title: "Status",
      dataIndex: "active",
      key: "active",
      render: (active: boolean) => <Tag color={active ? "green" : "default"}>{active ? "Active" : "Inactive"}</Tag>
    },
    {
      title: "Actions",
      key: "actions",
      width: 160,
      render: (_, record) =>
        record.active ? (
          <Popconfirm
            title="Deactivate face profile?"
            okText="Deactivate"
            okButtonProps={{ danger: true }}
            onConfirm={() => faceDeactivateMutation.mutate(record.id)}
          >
            <Button danger icon={<PoweroffOutlined />} loading={faceDeactivateMutation.isPending}>
              Deactivate
            </Button>
          </Popconfirm>
        ) : null
    }
  ];

  if (employeeQuery.isLoading) {
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
          <Typography.Title level={3}>Employee Profile</Typography.Title>
          <Typography.Text type="secondary">Profile overview, attendance tools, Face ID, credentials, and work schedule.</Typography.Text>
        </div>
        {employee ? (
          <Button icon={<EditOutlined />} type="primary" onClick={() => navigate(`/employees/${employee.id}/edit`)}>
            Edit profile
          </Button>
        ) : null}
      </div>

      {employeeQuery.isError ? <Alert type="error" message="Failed to load employee" showIcon /> : null}

      {employee ? (
        <>
          <Card>
            <div className="employee-profile-header">
              <Avatar size={96} src={photoSrc} icon={<UserOutlined />} />
              <Space direction="vertical" size={4}>
                <Typography.Text strong>
                  {employee.firstName} {employee.lastName}
                </Typography.Text>
                <Typography.Text type="secondary">Employee photo is used as the profile image and can later support Face ID enrollment.</Typography.Text>
                <Space wrap>
                  <Upload
                    accept="image/png,image/jpeg"
                    beforeUpload={(file) => {
                      photoMutation.mutate(file);
                      return false;
                    }}
                    disabled={photoMutation.isPending}
                    showUploadList={false}
                  >
                    <Button icon={<UploadOutlined />} loading={photoMutation.isPending}>
                      Upload Photo
                    </Button>
                  </Upload>
                  <Button icon={<CameraOutlined />} onClick={() => setPhotoCameraOpen(true)}>
                    Take Photo
                  </Button>
                  <Button
                    icon={<CloudUploadOutlined />}
                    disabled={!employee.photoUrl}
                    loading={hikvisionSyncMutation.isPending}
                    onClick={() => hikvisionSyncMutation.mutate()}
                  >
                    Send to Hikvision
                  </Button>
                </Space>
              </Space>
            </div>
          </Card>

          <Tabs
            className="employee-workspace-tabs"
            items={[
              {
                key: "profile",
                label: "Profil",
                children: (
                  <Card>
                    <Descriptions
                      bordered
                      column={{ xs: 1, sm: 1, md: 2 }}
                      title={
                        <Space>
                          <span>
                            {employee.firstName} {employee.lastName}
                          </span>
                          <Tag color={employee.active ? "green" : "default"}>{employee.active ? "Active" : "Inactive"}</Tag>
                        </Space>
                      }
                    >
                      <Descriptions.Item label="Employee number">{employee.employeeCode}</Descriptions.Item>
                      <Descriptions.Item label="Employment type">{display(employee.employmentType)}</Descriptions.Item>
                      <Descriptions.Item label="Company">
                        {companiesQuery.data?.find((company) => company.id === employee.companyId)?.name ?? employee.companyId}
                      </Descriptions.Item>
                      <Descriptions.Item label="Branch">{branchQuery.data?.name ?? employee.branchId}</Descriptions.Item>
                      <Descriptions.Item label="First name">{employee.firstName}</Descriptions.Item>
                      <Descriptions.Item label="Last name">{employee.lastName}</Descriptions.Item>
                      <Descriptions.Item label="Middle name">{display(employee.middleName)}</Descriptions.Item>
                      <Descriptions.Item label="Position">{display(employee.position)}</Descriptions.Item>
                      <Descriptions.Item label="Phone">{display(employee.phone)}</Descriptions.Item>
                      <Descriptions.Item label="Login email">{display(employee.email)}</Descriptions.Item>
                      <Descriptions.Item label="Hire date">{display(employee.hiredDate ?? employee.hireDate)}</Descriptions.Item>
                      <Descriptions.Item label="Birth date">{display(employee.birthDate)}</Descriptions.Item>
                      <Descriptions.Item label="Salary">{display(employee.salary)}</Descriptions.Item>
                    </Descriptions>
                  </Card>
                )
              },
              {
                key: "schedule",
                label: "Ish jadvali",
                children: <EmployeeScheduleCard employeeId={employee.id} />
              },
              {
                key: "credentials",
                label: "Ruxsat ma'lumotlari",
                children: (
                  <Card
                    title="Credentials"
                    extra={
                      <Button icon={<PlusOutlined />} onClick={() => navigate(`/credentials/new?employeeId=${employee.id}`)}>
                        Add Credential
                      </Button>
                    }
                  >
                    {credentialsQuery.isError ? <Alert type="error" message="Failed to load credentials" showIcon /> : null}
                    <Table
                      rowKey="id"
                      columns={credentialColumns}
                      dataSource={credentialsQuery.data ?? []}
                      loading={credentialsQuery.isLoading}
                      locale={{ emptyText: <Empty description="No credentials yet" /> }}
                      pagination={false}
                    />
                  </Card>
                )
              },
              {
                key: "face",
                label: "Face ID",
                children: (
                  <Card
                    title="Face Enrollment"
                    extra={
                      <Space wrap>
                        <Upload
                          accept="image/png,image/jpeg,image/webp"
                          beforeUpload={(file) => {
                            faceEnrollMutation.mutate(file);
                            return false;
                          }}
                          disabled={faceEnrollMutation.isPending}
                          showUploadList={false}
                        >
                          <Button icon={<UploadOutlined />} loading={faceEnrollMutation.isPending}>
                            Upload Face
                          </Button>
                        </Upload>
                        <Button
                          icon={<CameraOutlined />}
                          type="primary"
                          loading={faceEnrollMutation.isPending}
                          onClick={() => setFaceCameraOpen(true)}
                        >
                          Enroll by Camera
                        </Button>
                      </Space>
                    }
                  >
                    <Space direction="vertical" size={12} className="full-width">
                      <Alert
                        type="info"
                        showIcon
                        message="Upload a clear single-face photo. This creates the embedding used by Camera Check."
                      />
                      {faceProfilesQuery.isError ? <Alert type="error" message="Failed to load face profiles" showIcon /> : null}
                      <Table
                        rowKey="id"
                        columns={faceProfileColumns}
                        dataSource={faceProfilesQuery.data ?? []}
                        loading={faceProfilesQuery.isLoading}
                        locale={{ emptyText: <Empty description="No face profiles enrolled yet" /> }}
                        pagination={false}
                      />
                    </Space>
                  </Card>
                )
              },
              {
                key: "attendance",
                label: "Davomat",
                children: (
                  <Card
                    title="Attendance History"
                    extra={<Button onClick={() => navigate(`/attendance/employee?employeeId=${employee.id}`)}>View Attendance History</Button>}
                  >
                    <Empty description="Open the attendance history page to review this employee's sessions." />
                  </Card>
                )
              }
            ]}
          />

          <CameraCaptureModal
            open={photoCameraOpen}
            title="Take Employee Photo"
            confirmText="Use Photo"
            loading={photoMutation.isPending}
            onCancel={() => setPhotoCameraOpen(false)}
            onCapture={(file) => photoMutation.mutate(file)}
          />

          <CameraCaptureModal
            open={faceCameraOpen}
            title="Enroll Face by Camera"
            confirmText="Enroll Face"
            loading={faceEnrollMutation.isPending}
            onCancel={() => setFaceCameraOpen(false)}
            onCapture={(file) => faceEnrollMutation.mutate(file)}
          />
        </>
      ) : (
        <Empty description="Employee not found" />
      )}
    </Space>
  );
}
