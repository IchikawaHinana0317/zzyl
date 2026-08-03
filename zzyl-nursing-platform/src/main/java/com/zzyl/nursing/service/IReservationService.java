package com.zzyl.nursing.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zzyl.common.core.page.TableDataInfo;
import com.zzyl.nursing.domain.Reservation;
import com.zzyl.nursing.dto.ReservationDto;
import com.zzyl.nursing.vo.TimeCountVo;

import java.util.List;

/**
 * 预约信息Service接口
 *
 * @author hinana
 * @date 2024-11-11
 */
public interface IReservationService extends IService<Reservation>
{
    /**
     * 查询取消预约次数
     *
     * @param userId
     * @return
     */
    int getCancelledReservationCount(Long userId);

    /**
     * 小程序端新增预约信息
     *
     * @param reservationDto 预约信息
     * @return 结果
     */
    int insertReservation(ReservationDto reservationDto);


    /**
     * 前端新增预约信息
     *
     * @param reservation 预约信息
     * @return 结果
     */
    int insertReservation(Reservation reservation);

    /**
     * 查询每个时间段剩余预约次数
     *
     * @return
     */
    List<TimeCountVo> countReservationsForEachTimeWithinTimeRange(Long time);

    /**
     * 分页查询预约信息
     *
     *
     * @return
     */
    List<Reservation> selectReservationList(Reservation reservation);

    /**
     * 取消预约
     *
     * @param id
     * @return
     */
    int cancelReservation(Long id);


    /**
     * 定时更新过期预约的状态
     */
    void updateReservationStatus();


    /**
     * 根据ID查预约情况
     *
     * @param id
     * @return 预约结果
     */
    Reservation selectReservationById(Long id);


    int updateReservation(Reservation reservation);

    int deleteReservationByIds(Long[] ids);
}