package com.company.sec13f.repository.mapper;

import com.company.sec13f.repository.entity.Company;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Company数据访问接口
 */
public interface CompanyMapper {
    
    /**
     * 插入新的Company记录
     * @param company Company实体
     * @return 影响行数
     */
    int insert(Company company);
    
    /**
     * 批量插入Company记录
     * @param companies Company列表
     * @return 影响行数
     */
    int batchInsert(@Param("companies") List<Company> companies);
    
    /**
     * 根据ID查询Company
     * @param id 主键ID
     * @return Company实体
     */
    Company selectById(@Param("id") Long id);
    
    /**
     * 根据CIK查询Company
     * @param cik 公司CIK
     * @return Company实体
     */
    Company selectByCik(@Param("cik") String cik);
    
    /**
     * 根据公司名称查询Company（模糊搜索）
     * @param companyName 公司名称关键字
     * @return Company列表
     */
    List<Company> selectByCompanyName(@Param("companyName") String companyName);
    
    /**
     * 查询所有活跃的公司
     * @return Company列表
     */
    List<Company> selectActiveCompanies();
    
    /**
     * 分页查询公司列表
     * @param offset 偏移量
     * @param limit 限制数量
     * @param keyword 搜索关键字（可为null）
     * @param isActive 是否活跃（可为null）
     * @param sortBy 排序字段
     * @param sortOrder 排序方向（ASC/DESC）
     * @return Company列表
     */
    List<Company> selectCompaniesWithPaging(@Param("offset") int offset, 
                                           @Param("limit") int limit,
                                           @Param("keyword") String keyword,
                                           @Param("isActive") Boolean isActive,
                                           @Param("sortBy") String sortBy,
                                           @Param("sortOrder") String sortOrder);
    
    /**
     * 统计公司总数
     * @param keyword 搜索关键字（可为null）
     * @param isActive 是否活跃（可为null）
     * @return 总数
     */
    long countCompanies(@Param("keyword") String keyword, @Param("isActive") Boolean isActive);
    
    /**
     * 更新Company记录
     * @param company Company实体
     * @return 影响行数
     */
    int update(Company company);
    
    /**
     * 更新公司的文件统计信息
     * @param cik 公司CIK
     * @param totalFilings 总文件数
     * @param lastFilingDate 最新文件日期
     * @return 影响行数
     */
    int updateFilingStats(@Param("cik") String cik, 
                         @Param("totalFilings") Integer totalFilings, 
                         @Param("lastFilingDate") LocalDate lastFilingDate);
    
    /**
     * 根据ID删除Company
     * @param id 主键ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);
    
    /**
     * 根据CIK删除Company
     * @param cik 公司CIK
     * @return 影响行数
     */
    int deleteByCik(@Param("cik") String cik);
    
    /**
     * 检查CIK是否存在
     * @param cik 公司CIK
     * @return 存在返回true，否则返回false
     */
    boolean existsByCik(@Param("cik") String cik);
    
    /**
     * 统计活跃公司数量
     * @return 活跃公司数量
     */
    long countActiveCompanies();
    
    /**
     * 统计总公司数量
     * @return 总公司数量
     */
    long countAllCompanies();
    
    /**
     * 获取行业统计信息
     * @return 行业统计Map列表
     */
    List<Map<String, Object>> getIndustryStats();
    
    /**
     * 获取部门统计信息
     * @return 部门统计Map列表
     */
    List<Map<String, Object>> getSectorStats();
    
    /**
     * 根据行业查询公司
     * @param industry 行业名称
     * @return Company列表
     */
    List<Company> selectByIndustry(@Param("industry") String industry);
    
    /**
     * 根据部门查询公司
     * @param sector 部门名称
     * @return Company列表
     */
    List<Company> selectBySector(@Param("sector") String sector);
    
    /**
     * 获取最近有文件更新的公司
     * @param days 天数
     * @param limit 限制数量
     * @return Company列表
     */
    List<Company> selectRecentlyActiveCompanies(@Param("days") int days, @Param("limit") int limit);
}