import { DeleteOutlined, EditOutlined, PlusOutlined, SettingOutlined } from "@ant-design/icons";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Alert, Button, Empty, Popconfirm, Space, Table, Tag, Typography, message } from "antd";
import type { ColumnsType } from "antd/es/table";
import { useNavigate } from "react-router-dom";
import { CompanyResponse, companyApi } from "../../features/company/api/companyApi";

export default function CompaniesPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const companiesQuery = useQuery({
    queryKey: ["companies"],
    queryFn: companyApi.listCompanies
  });

  const deleteMutation = useMutation({
    mutationFn: companyApi.deleteCompany,
    onSuccess: () => {
      message.success("Company deactivated");
      queryClient.invalidateQueries({ queryKey: ["companies"] });
    },
    onError: () => {
      message.error("Failed to deactivate company");
    }
  });

  const columns: ColumnsType<CompanyResponse> = [
    {
      title: "Name",
      dataIndex: "name",
      key: "name"
    },
    {
      title: "INN",
      dataIndex: "inn",
      key: "inn",
      render: (value?: string | null) => value || "-"
    },
    {
      title: "Phone",
      dataIndex: "phone",
      key: "phone",
      render: (value?: string | null) => value || "-"
    },
    {
      title: "Email",
      dataIndex: "email",
      key: "email",
      render: (value?: string | null) => value || "-"
    },
    {
      title: "Plan",
      dataIndex: "plan",
      key: "plan",
      render: (value?: string | null) => <Tag color="blue">{value || "FREE"}</Tag>
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
      width: 220,
      render: (_, record) => (
        <Space>
          <Button icon={<EditOutlined />} onClick={() => navigate(`/companies/${record.id}/edit`)}>
            Edit
          </Button>
          <Button icon={<SettingOutlined />} onClick={() => navigate(`/companies/${record.id}/settings`)}>
            Settings
          </Button>
          <Popconfirm
            title="Deactivate company?"
            description="This will soft-delete the company."
            okText="Deactivate"
            okButtonProps={{ danger: true }}
            onConfirm={() => deleteMutation.mutate(record.id)}
          >
            <Button danger icon={<DeleteOutlined />} loading={deleteMutation.isPending}>
              Delete
            </Button>
          </Popconfirm>
        </Space>
      )
    }
  ];

  return (
    <Space direction="vertical" size={16} className="page-stack">
      <div className="page-toolbar">
        <div>
          <Typography.Title level={3}>Companies</Typography.Title>
          <Typography.Text type="secondary">Manage company profiles and settings.</Typography.Text>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate("/companies/new")}>
          Create Company
        </Button>
      </div>

      {companiesQuery.isError ? <Alert type="error" message="Failed to load companies" showIcon /> : null}

      <Table
        rowKey="id"
        columns={columns}
        dataSource={companiesQuery.data ?? []}
        loading={companiesQuery.isLoading}
        locale={{ emptyText: <Empty description="No companies yet" /> }}
        pagination={{ pageSize: 10 }}
      />
    </Space>
  );
}
