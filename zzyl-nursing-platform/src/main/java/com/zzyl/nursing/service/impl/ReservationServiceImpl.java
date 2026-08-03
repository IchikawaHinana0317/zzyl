package com.zzyl.nursing.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzyl.common.core.page.TableDataInfo;
import com.zzyl.common.exception.base.BaseException;
import com.zzyl.common.utils.DateUtils;
import com.zzyl.common.utils.UserThreadLocal;
import com.zzyl.nursing.domain.Reservation;
import com.zzyl.nursing.dto.ReservationDto;
import com.zzyl.nursing.mapper.ReservationMapper;
import com.zzyl.nursing.service.IReservationService;
import com.zzyl.nursing.vo.TimeCountVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 预约信息Service业务层处理
 *
 * @author hinana
 * @date 2024-11-11
 */
@Service
public class ReservationServiceImpl extends ServiceImpl<ReservationMapper, Reservation> implements IReservationService
{
    @Autowired
    private ReservationMapper reservationMapper;

    /**
     * 查询取消预约次数
     *
     * @param userId    用户id
     * @return  取消预约次数
     */
    @Override
    public int getCancelledReservationCount(Long userId) {
        // 2024/11/11 00:00:00
        LocalDateTime startTime = LocalDate.now().atStartOfDay();
        // 2024/10/12 00:00:00
        LocalDateTime endTime = startTime.plusDays(1);

        int count = reservationMapper.getCancelledReservationCount(userId, startTime, endTime);
        return count;
    }

    /**
     * 查询每个时间段剩余预约次数
     *
     * @return 结果
     */
    @Override
    public List<TimeCountVo> countReservationsForEachTimeWithinTimeRange(Long time) {
        LocalDateTime localDateTime = LocalDateTimeUtil.of(time);

        LocalDateTime startTime = localDateTime.toLocalDate().atStartOfDay();

        LocalDateTime endTime = startTime.plusDays(1);
        List<TimeCountVo> timeCountVoList = reservationMapper.countReservationsForEachTimeWithinTimeRange(startTime, endTime);
        return timeCountVoList;
    }

    /**
     * 分页查询预约信息

     * @return
     */
    @Override
    public List<Reservation> selectReservationList(Reservation reservation) {
        return reservationMapper.selectReservationList(reservation);
    }



    /**
     * 取消预约
     *
     * @param id
     * @return
     */
    @Override
    public int cancelReservation(Long id) {
        Reservation reservation = getById(id);
        if (ObjectUtil.isEmpty(reservation)) {
            throw new BaseException("预约不存在");
        }
        reservation.setStatus(2);
        reservation.setUpdateBy(String.valueOf(UserThreadLocal.getUserId()));
        return updateById(reservation) ? 1 : 0;
    }

    /**
     * 新增预约信息
     *
     * @param reservationDto 预约信息
     * @return 结果
     */
    @Override
    public int insertReservation(ReservationDto reservationDto) {
        Long userId = UserThreadLocal.getUserId();
        Reservation reservation = BeanUtil.toBean(reservationDto, Reservation.class);
        reservation.setCreateBy(String.valueOf(userId));
        reservation.setUpdateBy(String.valueOf(userId));
        reservation.setStatus(0);
        try {
            return save(reservation) ? 1 : 0;
        } catch (Exception e) {
            throw new BaseException("不能重复预约哦");
        }
    }

    /**
     * 定时更新过期预约的状态
     */
    @Override
    public void updateReservationStatus() {
        LocalDateTime expireTime = LocalDateTime.now().minusMinutes(30);
        reservationMapper.updateReservationStatus(expireTime);
    }

    @Override
    public Reservation selectReservationById(Long id) {
        return getById(id);
    }


    @Override
    public int insertReservation(Reservation reservation) {
        reservation.setCreateTime(DateUtils.getNowDate());
        return save(reservation) ? 1 : 0;

    }

    @Override
    public int updateReservation(Reservation reservation) {
        reservation.setUpdateTime(DateUtils.getNowDate());
        return updateById(reservation) ? 1 : 0;

    }

    @Override
    public int deleteReservationByIds(Long[] ids) {
        return removeByIds(Arrays.asList(ids)) ? 1 : 0;

    }
}