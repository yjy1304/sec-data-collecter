package com.company.sec13f.repository.mapper;

import com.company.sec13f.repository.entity.Holding;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Holding数据访问接口
 */
public interface HoldingMapper {
    
    /**
     * 插入新的Holding记录
     * @param holding Holding实体
     * @return 影响行数
     */
    int insert(Holding holding);
    
    /**
     * 批量插入Holding记录
     * @param holdings Holding列表
     * @return 影响行数
     */
    int batchInsert(@Param("holdings") List<Holding> holdings);
    
    /**
     * 根据ID查询Holding
     * @param id 主键ID
     * @return Holding实体
     */
    Holding selectById(@Param("id") Long id);
    
    /**
     * 根据Filing ID查询所有Holding
     * @param filingId Filing主键ID
     * @return Holding列表
     */
    List<Holding> selectByFilingId(@Param("filingId") Long filingId);
    
    /**
     * 根据CIK查询所有Holding（带Filing信息）
     * @param cik 公司CIK
     * @return Holding列表
     */
    List<Holding> selectByCikWithFiling(@Param("cik") String cik);
    
    /**
     * 根据CUSIP查询Holding
     * @param cusip CUSIP代码
     * @return Holding列表
     */
    List<Holding> selectByCusip(@Param("cusip") String cusip);
    
    /**
     * 根据发行人名称查询Holding
     * @param nameOfIssuer 发行人名称（支持模糊查询）
     * @return Holding列表
     */
    List<Holding> selectByIssuerName(@Param("nameOfIssuer") String nameOfIssuer);
    
    /**
     * 查询指定CIK的Top N持仓（按价值排序）
     * @param cik 公司CIK
     * @param limit 限制数量
     * @return Holding列表
     */
    List<Holding> selectTopHoldingsByCik(@Param("cik") String cik, @Param("limit") int limit);
    
    /**
     * 更新Holding记录
     * @param holding Holding实体
     * @return 影响行数
     */
    int update(Holding holding);
    
    /**
     * 根据ID删除Holding
     * @param id 主键ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);
    
    /**
     * 根据Filing ID删除所有相关Holding
     * @param filingId Filing主键ID
     * @return 影响行数
     */
    int deleteByFilingId(@Param("filingId") Long filingId);
    
    /**
     * 统计Holding总数
     * @return 总数
     */
    long countAll();
    
    /**
     * 根据Filing ID统计Holding数量
     * @param filingId Filing主键ID
     * @return 数量
     */
    long countByFilingId(@Param("filingId") Long filingId);
    
    /**
     * 根据CIK统计Holding数量
     * @param cik 公司CIK
     * @return 数量
     */
    long countByCik(@Param("cik") String cik);
    
    /**
     * 查询指定CIK的持仓总价值
     * @param cik 公司CIK
     * @return 总价值
     */
    java.math.BigDecimal sumValueByCik(@Param("cik") String cik);
    
    /**
     * 根据CIK查询持仓（带Filing信息和筛选条件）
     * @param cik 公司CIK
     * @param minValue 最小价值筛选
     * @param search 搜索关键字
     * @param sortBy 排序字段
     * @param sortOrder 排序方向
     * @return Holding列表
     */
    List<Holding> selectByCikWithFilingFiltered(@Param("cik") String cik, 
                                               @Param("minValue") Double minValue, 
                                               @Param("search") String search, 
                                               @Param("sortBy") String sortBy, 
                                               @Param("sortOrder") String sortOrder);
    
    /**
     * 根据CIK查询持仓（带Filing信息和筛选条件，支持日期筛选）
     * @param cik 公司CIK
     * @param minValue 最小价值筛选
     * @param search 搜索关键字
     * @param sortBy 排序字段
     * @param sortOrder 排序方向
     * @param filingDateFrom 报告日期开始
     * @param filingDateTo 报告日期结束
     * @return Holding列表
     */
    List<Holding> selectByCikWithFilingDateFiltered(@Param("cik") String cik, 
                                                   @Param("minValue") Double minValue, 
                                                   @Param("search") String search, 
                                                   @Param("sortBy") String sortBy, 
                                                   @Param("sortOrder") String sortOrder,
                                                   @Param("filingDateFrom") String filingDateFrom,
                                                   @Param("filingDateTo") String filingDateTo);
    
    /**
     * 根据CIK查询持仓用于导出
     * @param cik 公司CIK
     * @return Holding列表
     */
    List<Holding> selectByCikWithFilingForExport(@Param("cik") String cik);
    
    /**
     * 计算所有持仓的总价值
     * @return 总价值
     */
    java.math.BigDecimal sumAllValues();
    
    /**
     * 根据CIK查询持仓（带Filing信息和筛选条件，支持日期筛选和报告期间筛选）
     * @param params 参数Map，包含cik, minValue, search, sortBy, sortOrder, filingDateFrom, filingDateTo, reportPeriodFrom, reportPeriodTo
     * @return Holding列表
     */
    List<Holding> selectByCikWithFilingAndReportPeriodFiltered(java.util.Map<String, Object> params);
}