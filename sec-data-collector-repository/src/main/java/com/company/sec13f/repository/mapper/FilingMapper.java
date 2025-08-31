package com.company.sec13f.repository.mapper;

import com.company.sec13f.repository.entity.Filing;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * Filing数据访问接口
 */
public interface FilingMapper {
    
    /**
     * 插入新的Filing记录
     * @param filing Filing实体
     * @return 影响行数
     */
    int insert(Filing filing);
    
    /**
     * 根据ID查询Filing
     * @param id 主键ID
     * @return Filing实体
     */
    Filing selectById(@Param("id") Long id);
    
    /**
     * 根据CIK查询所有Filing
     * @param cik 公司CIK
     * @return Filing列表
     */
    List<Filing> selectByCik(@Param("cik") String cik);
    
    /**
     * 根据Accession Number和Form File查询Filing
     * @param accessionNumber 申报编号
     * @param formFile 表单文件
     * @return Filing实体
     */
    Filing selectByAccessionAndFormFile(@Param("accessionNumber") String accessionNumber, 
                                       @Param("formFile") String formFile);
    
    /**
     * 根据Accession Number查询Filing（返回第一个匹配项）
     * @param accessionNumber 申报编号
     * @return Filing实体
     */
    Filing selectByAccessionNumber(@Param("accessionNumber") String accessionNumber);
    
    /**
     * 查询所有Filing（带持仓数量统计）
     * @return Filing列表
     */
    List<Filing> selectAllWithHoldingsCount();
    
    /**
     * 更新Filing记录
     * @param filing Filing实体
     * @return 影响行数
     */
    int update(Filing filing);
    
    /**
     * 根据ID删除Filing
     * @param id 主键ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);
    
    /**
     * 检查Filing是否存在
     * @param accessionNumber 申报编号
     * @return 存在返回true，否则返回false
     */
    boolean existsByAccessionNumber(@Param("accessionNumber") String accessionNumber);
    
    /**
     * 统计Filing总数
     * @return 总数
     */
    long countAll();
    
    /**
     * 根据CIK统计Filing数量
     * @param cik 公司CIK
     * @return 数量
     */
    long countByCik(@Param("cik") String cik);
    
    /**
     * 获取所有有持仓数据的公司列表
     * @param cik CIK筛选条件（可为null）
     * @param name 公司名称筛选条件（可为null）
     * @param sortBy 排序字段
     * @return 公司列表，包含统计信息
     */
    List<Map<String, Object>> selectCompaniesWithHoldings(@Param("cik") String cik, @Param("name") String name, @Param("sortBy") String sortBy);
    
    /**
     * 根据CIK获取最新的Filing
     * @param cik 公司CIK
     * @return 最新的Filing
     */
    Filing selectLatestByCik(@Param("cik") String cik);
    
    /**
     * 获取最新的Filing
     * @return 最新的Filing
     */
    Filing selectLatest();
    
    /**
     * 统计不同公司数量
     * @return 公司数量
     */
    long countDistinctCompanies();
}