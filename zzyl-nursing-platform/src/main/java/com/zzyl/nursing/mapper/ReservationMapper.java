package com.zzyl.nursing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zzyl.nursing.domain.Reservation;
import com.zzyl.nursing.vo.TimeCountVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 预约信息Mapper接口
 *
 * @author hinana
 * @date 2024-11-11
 */
@Mapper
public interface ReservationMapper extends BaseMapper<Reservation>
{
    int getCancelledReservationCount(@Param("userId") Long userId,@Param("startTime") LocalDateTime startTime,@Param("endTime") LocalDateTime endTime);


    List<TimeCountVo> countReservationsForEachTimeWithinTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime")  LocalDateTime endTime);


    int updateReservationStatus(@Param("expireTime") LocalDateTime expireTime);

    List<Reservation> selectReservationList(Reservation reservation);


}