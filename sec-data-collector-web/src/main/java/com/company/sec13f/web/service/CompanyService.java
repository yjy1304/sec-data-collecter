package com.company.sec13f.web.service;

import com.company.sec13f.repository.MyBatisSessionFactory;
import com.company.sec13f.repository.entity.Company;
import com.company.sec13f.repository.mapper.CompanyMapper;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 公司维护服务类
 */
@Service
public class CompanyService {
    
    private static final Logger logger = LoggerFactory.getLogger(CompanyService.class);
    
    /**
     * 分页查询公司列表
     */
    public Map<String, Object> getCompanies(int page, int size, String keyword, Boolean isActive, 
                                          String sortBy, String sortOrder) {
        try (SqlSession session = MyBatisSessionFactory.openSession()) {
            CompanyMapper mapper = session.getMapper(CompanyMapper.class);
            
            int offset = (page - 1) * size;
            
            // 查询公司列表
            List<Company> companies = mapper.selectCompaniesWithPaging(
                offset, size, keyword, isActive, sortBy, sortOrder);
            
            // 查询总数
            long totalCount = mapper.countCompanies(keyword, isActive);
            int totalPages = (int) Math.ceil((double) totalCount / size);
            
            // 构造响应
            Map<String, Object> result = new HashMap<>();
            result.put("companies", companies);
            result.put("totalCount", totalCount);
            result.put("totalPages", totalPages);
            result.put("currentPage", page);
            result.put("pageSize", size);
            
            logger.info("📋 返回公司列表: {} 条记录", companies.size());
            return result;
            
        } catch (Exception e) {
            logger.error("Error getting companies", e);
            throw new RuntimeException("Failed to get companies: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取统计信息
     */
    public Map<String, Object> getCompanyStats() {
        try (SqlSession session = MyBatisSessionFactory.openSession()) {
            CompanyMapper mapper = session.getMapper(CompanyMapper.class);
            
            long totalCompanies = mapper.countAllCompanies();
            long activeCompanies = mapper.countActiveCompanies();
            
            // 获取行业统计
            List<Map<String, Object>> industryStats = mapper.getIndustryStats();
            long totalIndustries = industryStats.size();
            
            // 获取最近30天活跃的公司数量
            List<Company> recentlyActive = mapper.selectRecentlyActiveCompanies(30, 1000);
            long recentlyActiveCount = recentlyActive.size();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalCompanies", totalCompanies);
            stats.put("activeCompanies", activeCompanies);
            stats.put("totalIndustries", totalIndustries);
            stats.put("recentlyActive", recentlyActiveCount);
            
            logger.info("📊 返回公司统计信息");
            return stats;
            
        } catch (Exception e) {
            logger.error("Error getting company stats", e);
            throw new RuntimeException("Failed to get company stats: " + e.getMessage(), e);
        }
    }
    
    /**
     * 根据ID获取公司信息
     */
    public Company getCompanyById(Long id) {
        try (SqlSession session = MyBatisSessionFactory.openSession()) {
            CompanyMapper mapper = session.getMapper(CompanyMapper.class);
            Company company = mapper.selectById(id);
            
            if (company != null) {
                logger.info("📋 返回公司信息: {}", company.getCompanyName());
            }
            return company;
            
        } catch (Exception e) {
            logger.error("Error getting company by id: " + id, e);
            throw new RuntimeException("Failed to get company: " + e.getMessage(), e);
        }
    }
    
    /**
     * 根据CIK获取公司信息
     */
    public Company getCompanyByCik(String cik) {
        try (SqlSession session = MyBatisSessionFactory.openSession()) {
            CompanyMapper mapper = session.getMapper(CompanyMapper.class);
            return mapper.selectByCik(cik);
            
        } catch (Exception e) {
            logger.error("Error getting company by cik: " + cik, e);
            throw new RuntimeException("Failed to get company by cik: " + e.getMessage(), e);
        }
    }
    
    /**
     * 添加公司
     */
    public Company addCompany(Company company) {
        try (SqlSession session = MyBatisSessionFactory.openSession()) {
            CompanyMapper mapper = session.getMapper(CompanyMapper.class);
            
            // 检查CIK是否已存在
            if (mapper.existsByCik(company.getCik())) {
                throw new IllegalArgumentException("CIK已存在: " + company.getCik());
            }
            
            int result = mapper.insert(company);
            session.commit();
            
            if (result > 0) {
                logger.info("✅ 添加公司成功: {} (CIK: {})", company.getCompanyName(), company.getCik());
                return company;
            } else {
                throw new RuntimeException("添加公司失败");
            }
            
        } catch (Exception e) {
            logger.error("Error adding company", e);
            if (e instanceof IllegalArgumentException) {
                throw e;
            }
            throw new RuntimeException("Failed to add company: " + e.getMessage(), e);
        }
    }
    
    /**
     * 更新公司
     */
    public Company updateCompany(Long id, Company company) {
        try (SqlSession session = MyBatisSessionFactory.openSession()) {
            CompanyMapper mapper = session.getMapper(CompanyMapper.class);
            
            // 检查公司是否存在
            Company existingCompany = mapper.selectById(id);
            if (existingCompany == null) {
                throw new IllegalArgumentException("公司不存在");
            }
            
            // 如果CIK发生变化，检查新CIK是否已存在
            if (!existingCompany.getCik().equals(company.getCik()) && 
                mapper.existsByCik(company.getCik())) {
                throw new IllegalArgumentException("CIK已存在: " + company.getCik());
            }
            
            company.setId(id);
            int result = mapper.update(company);
            session.commit();
            
            if (result > 0) {
                logger.info("✅ 更新公司成功: {} (CIK: {})", company.getCompanyName(), company.getCik());
                return company;
            } else {
                throw new RuntimeException("更新公司失败");
            }
            
        } catch (Exception e) {
            logger.error("Error updating company with id: " + id, e);
            if (e instanceof IllegalArgumentException) {
                throw e;
            }
            throw new RuntimeException("Failed to update company: " + e.getMessage(), e);
        }
    }
    
    /**
     * 删除公司
     */
    public boolean deleteCompany(Long id) {
        try (SqlSession session = MyBatisSessionFactory.openSession()) {
            CompanyMapper mapper = session.getMapper(CompanyMapper.class);
            
            // 检查公司是否存在
            Company existingCompany = mapper.selectById(id);
            if (existingCompany == null) {
                throw new IllegalArgumentException("公司不存在");
            }
            
            int result = mapper.deleteById(id);
            session.commit();
            
            if (result > 0) {
                logger.info("✅ 删除公司成功: {} (CIK: {})", existingCompany.getCompanyName(), existingCompany.getCik());
                return true;
            } else {
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Error deleting company with id: " + id, e);
            if (e instanceof IllegalArgumentException) {
                throw e;
            }
            throw new RuntimeException("Failed to delete company: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取公司列表用于导出
     */
    public List<Company> getCompaniesForExport(String keyword, Boolean isActive) {
        try (SqlSession session = MyBatisSessionFactory.openSession()) {
            CompanyMapper mapper = session.getMapper(CompanyMapper.class);
            
            // 获取所有符合条件的公司（限制10000条）
            List<Company> companies = mapper.selectCompaniesWithPaging(
                0, 10000, keyword, isActive, "companyName", "ASC");
            
            logger.info("📊 导出公司列表: {} 条记录", companies.size());
            return companies;
            
        } catch (Exception e) {
            logger.error("Error getting companies for export", e);
            throw new RuntimeException("Failed to get companies for export: " + e.getMessage(), e);
        }
    }
    
    /**
     * 批量添加公司
     */
    public int batchAddCompanies(List<Company> companies) {
        try (SqlSession session = MyBatisSessionFactory.openSession()) {
            CompanyMapper mapper = session.getMapper(CompanyMapper.class);
            
            // 检查CIK重复
            for (Company company : companies) {
                if (mapper.existsByCik(company.getCik())) {
                    throw new IllegalArgumentException("CIK已存在: " + company.getCik());
                }
            }
            
            int result = mapper.batchInsert(companies);
            session.commit();
            
            logger.info("✅ 批量添加公司成功: {} 条记录", result);
            return result;
            
        } catch (Exception e) {
            logger.error("Error batch adding companies", e);
            if (e instanceof IllegalArgumentException) {
                throw e;
            }
            throw new RuntimeException("Failed to batch add companies: " + e.getMessage(), e);
        }
    }
    
    /**
     * 根据行业获取公司列表
     */
    public List<Company> getCompaniesByIndustry(String industry) {
        try (SqlSession session = MyBatisSessionFactory.openSession()) {
            CompanyMapper mapper = session.getMapper(CompanyMapper.class);
            return mapper.selectByIndustry(industry);
            
        } catch (Exception e) {
            logger.error("Error getting companies by industry: " + industry, e);
            throw new RuntimeException("Failed to get companies by industry: " + e.getMessage(), e);
        }
    }
    
    /**
     * 根据部门获取公司列表
     */
    public List<Company> getCompaniesBySector(String sector) {
        try (SqlSession session = MyBatisSessionFactory.openSession()) {
            CompanyMapper mapper = session.getMapper(CompanyMapper.class);
            return mapper.selectBySector(sector);
            
        } catch (Exception e) {
            logger.error("Error getting companies by sector: " + sector, e);
            throw new RuntimeException("Failed to get companies by sector: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取行业统计信息
     */
    public List<Map<String, Object>> getIndustryStats() {
        try (SqlSession session = MyBatisSessionFactory.openSession()) {
            CompanyMapper mapper = session.getMapper(CompanyMapper.class);
            return mapper.getIndustryStats();
            
        } catch (Exception e) {
            logger.error("Error getting industry stats", e);
            throw new RuntimeException("Failed to get industry stats: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取部门统计信息
     */
    public List<Map<String, Object>> getSectorStats() {
        try (SqlSession session = MyBatisSessionFactory.openSession()) {
            CompanyMapper mapper = session.getMapper(CompanyMapper.class);
            return mapper.getSectorStats();
            
        } catch (Exception e) {
            logger.error("Error getting sector stats", e);
            throw new RuntimeException("Failed to get sector stats: " + e.getMessage(), e);
        }
    }
}