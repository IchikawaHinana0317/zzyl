package com.zzyl.nursing.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzyl.common.constant.CacheConstants;
import com.zzyl.nursing.domain.Room;
import com.zzyl.nursing.mapper.RoomMapper;
import com.zzyl.nursing.service.IRoomService;
import com.zzyl.nursing.vo.RoomVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 房间Service业务层处理
 *
 * @author hinana
 * @date 2024-04-26
 */
@Service
@Slf4j
public class RoomServiceImpl extends ServiceImpl<RoomMapper, Room> implements IRoomService {
    @Autowired
    private RoomMapper roomMapper;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 查询房间
     *
     * @param id 房间主键
     * @return 房间
     */
    @Override
    public Room selectRoomById(Long id) {
        return getById(id);
    }

    /**
     * 查询房间列表
     *
     * @param room 房间
     * @return 房间
     */
    @Override
    public List<Room> selectRoomList(Room room) {
        return roomMapper.selectRoomList(room);
    }

    /**
     * 新增房间
     *
     * @param room 房间
     * @return 结果
     */
    @Override
    public int insertRoom(Room room) {
        int flag = roomMapper.insertRoom(room);
        log.info("因为进行新增操作，删除原本旧缓存！");
        deleteCache();
        return flag;

    }

    /**
     * 修改房间
     *
     * @param room 房间
     * @return 结果
     */
    @Override
    public int updateRoom(Room room) {
        int flag = roomMapper.updateRoom(room);
        log.info("因为进行修改操作，删除原本旧缓存！");
        deleteCache();
        return flag;
    }

    /**
     * 批量删除房间
     *
     * @param ids 需要删除的房间主键
     * @return 结果
     */
    @Override
    public int deleteRoomByIds(Long[] ids) {
        int flag = roomMapper.deleteRoomByIds(ids);
        log.info("因为进行批量删除操作，删除原本旧缓存！");
        deleteCache();
        return flag;

    }

    /**
     * 根据楼层 id 获取房间视图对象列表
     *
     * @param floorId 楼层ID
     * @return
     */
    @Override
    public List<RoomVo> getRoomsByFloorId(Long floorId) {
        return roomMapper.selectByFloorId(floorId);
    }


    /**
     * 获取所有房间（负责老人）
     *
     * @param floorId 楼层ID
     * @return
     */
    @Override
    public List<RoomVo> getRoomsWithNurByFloorId(Long floorId) {
        return roomMapper.selectByFloorIdWithNur(floorId);
    }

    /**
     *
     * @param id 房间ID
     * @return 房间相关数据
     */
    @Override
    public RoomVo getRoomById(Long id) {
        //查询缓存
        RoomVo room=(RoomVo) redisTemplate.opsForValue().get(CacheConstants.ROOM_DATA_KEY);
        //如果有缓存，直接用
        if(ObjectUtil.isNotEmpty(room))
        {
            log.info("查询缓存命中成功!");
            return room;
        }
        //如果没有，则查数据库
        room = roomMapper.getRoomById(id);
        log.info("缓存中没有数据，重新从数据库加载到缓存！");
        redisTemplate.opsForValue().set(CacheConstants.ROOM_DATA_KEY,room);
        return room;
    }

    /**
     * 删除缓存
     */
    private void deleteCache() {
        // 删除缓存
        redisTemplate.delete(CacheConstants.ROOM_DATA_KEY);
    }
}
