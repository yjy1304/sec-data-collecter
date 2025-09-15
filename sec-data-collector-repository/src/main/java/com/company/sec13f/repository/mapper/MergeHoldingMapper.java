package com.company.sec13f.repository.mapper;

import com.company.sec13f.repository.entity.MergeHolding;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 合并持仓数据访问接口
 */
public interface MergeHoldingMapper {
    
    /**
     * 插入合并持仓记录
     * @param mergeHolding 合并持仓实体
     * @return 影响行数
     */
    int insert(MergeHolding mergeHolding);
    
    /**
     * 批量插入合并持仓记录
     * @param mergeHoldings 合并持仓实体列表
     * @return 影响行数
     */
    int batchInsert(@Param("mergeHoldings") List<MergeHolding> mergeHoldings);
    
    /**
     * 根据filing_id删除合并持仓记录
     * @param filingId 申报ID
     * @return 影响行数
     */
    int deleteByFilingId(@Param("filingId") Long filingId);
    
    /**
     * 根据filing_id和cusip查询聚合后的持仓数据
     * @param filingId 申报ID
     * @return 聚合后的持仓数据列表
     */
    List<Map<String, Object>> selectAggregatedHoldingsByFilingId(@Param("filingId") Long filingId);
    
    /**
     * 根据filing_id查询合并后的持仓记录
     * @param filingId 申报ID
     * @return 合并持仓记录列表
     */
    List<MergeHolding> selectByFilingId(@Param("filingId") Long filingId);
    
    /**
     * 根据ID查询合并持仓记录
     * @param id 主键ID
     * @return 合并持仓记录
     */
    MergeHolding selectById(@Param("id") Long id);
    
    /**
     * 统计指定filing_id的合并持仓记录数量
     * @param filingId 申报ID
     * @return 记录数量
     */
    long countByFilingId(@Param("filingId") Long filingId);
    
    /**
     * 统计总记录数
     * @return 总记录数
     */
    long countAll();
    
    /**
     * 根据CUSIP和filing_id查询是否已存在合并记录
     * @param filingId 申报ID
     * @param cusip CUSIP码
     * @return 是否存在
     */
    boolean existsByFilingIdAndCusip(@Param("filingId") Long filingId, @Param("cusip") String cusip);
}