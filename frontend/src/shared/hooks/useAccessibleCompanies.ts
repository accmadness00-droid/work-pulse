import { useQuery } from "@tanstack/react-query";
import { companyApi, type CompanyResponse } from "../../features/company/api/companyApi";
import { useAuth } from "../auth/useAuth";

function scopedCompany(id: string): CompanyResponse {
  return {
    id,
    name: "My company",
    active: true
  };
}

export function useAccessibleCompanies() {
  const { user } = useAuth();

  return useQuery({
    queryKey: ["companies", "accessible", user?.role, user?.companyId],
    enabled: Boolean(user),
    queryFn: async () => {
      if (user?.role === "SUPER_ADMIN") {
        return companyApi.listCompanies();
      }
      if (!user?.companyId) {
        return [];
      }
      if (user.role === "COMPANY_ADMIN") {
        return [await companyApi.getCompany(user.companyId)];
      }
      return [scopedCompany(user.companyId)];
    }
  });
}
