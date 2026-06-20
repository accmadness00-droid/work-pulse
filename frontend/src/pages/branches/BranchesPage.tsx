import { DeleteOutlined, EditOutlined, PlusOutlined, ScheduleOutlined } from "@ant-design/icons";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Alert, Button, Empty, Popconfirm, Select, Space, Table, Tag, Typography, message } from "antd";
import type { ColumnsType } from "antd/es/table";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { branchApi, BranchResponse } from "../../features/branch/api/branchApi";
import { useAccessibleCompanies } from "../../shared/hooks/useAccessibleCompanies";

export default function BranchesPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [companyId, setCompanyId] = useState<string>();

  const companiesQuery = useAccessibleCompanies();

  useEffect(() => {
    if (!companyId && companiesQuery.data?.length) {
      setCompanyId(companiesQuery.data[0].id);
    }
  }, [companyId, companiesQuery.data]);

  const branchesQuery = useQuery({
    queryKey: ["branches", companyId],
    queryFn: () => branchApi.listBranches(companyId!),
    enabled: Boolean(companyId)
  });

  const deleteMutation = useMutation({
    mutationFn: branchApi.deleteBranch,
    onSuccess: () => {
      message.success("Branch deactivated");
      queryClient.invalidateQueries({ queryKey: ["branches"] });
    },
    onError: () => {
      message.error("Failed to deactivate branch");
    }
  });

  const columns: ColumnsType<BranchResponse> = [
    {
      title: "Name",
      dataIndex: "name",
      key: "name"
    },
    {
      title: "Address",
      dataIndex: "address",
      key: "address",
      render: (value?: string | null) => value || "-"
    },
    {
      title: "Latitude",
      dataIndex: "latitude",
      key: "latitude",
      render: (value?: number | null) => value ?? "-"
    },
    {
      title: "Longitude",
      dataIndex: "longitude",
      key: "longitude",
      render: (value?: number | null) => value ?? "-"
    },
    {
      title: "Radius",
      dataIndex: "geofenceRadiusMeters",
      key: "geofenceRadiusMeters",
      render: (value?: number | null) => (value ? `${value} m` : "-")
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
          <Button icon={<EditOutlined />} onClick={() => navigate(`/branches/${record.id}/edit`)}>
            Edit
          </Button>
          <Button icon={<ScheduleOutlined />} onClick={() => navigate(`/branches/${record.id}/schedule`)}>
            Schedule
          </Button>
          <Popconfirm
            title="Deactivate branch?"
            description="This will soft-delete the branch."
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
          <Typography.Title level={3}>Branches</Typography.Title>
          <Typography.Text type="secondary">Manage office branches for a selected company.</Typography.Text>
        </div>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          disabled={!companyId}
          onClick={() => navigate(companyId ? `/branches/new?companyId=${companyId}` : "/branches/new")}
        >
          Create Branch
        </Button>
      </div>

      <div className="filter-bar">
        <Select
          placeholder="Select company"
          loading={companiesQuery.isLoading}
          value={companyId}
          onChange={setCompanyId}
          options={(companiesQuery.data ?? []).map((company) => ({
            value: company.id,
            label: company.name
          }))}
          className="company-filter"
        />
      </div>

      {companiesQuery.isError ? <Alert type="error" message="Failed to load companies" showIcon /> : null}
      {branchesQuery.isError ? <Alert type="error" message="Failed to load branches" showIcon /> : null}

      <Table
        rowKey="id"
        columns={columns}
        dataSource={branchesQuery.data ?? []}
        loading={companiesQuery.isLoading || branchesQuery.isLoading}
        locale={{ emptyText: <Empty description={companyId ? "No branches yet" : "Select a company first"} /> }}
        pagination={{ pageSize: 10 }}
      />
    </Space>
  );
}
