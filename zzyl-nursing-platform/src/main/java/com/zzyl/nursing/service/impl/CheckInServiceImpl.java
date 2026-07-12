package com.zzyl.nursing.service.impl;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import cn.hutool.core.util.IdcardUtil;
import com.alibaba.fastjson2.JSON;
import com.zzyl.common.exception.base.BaseException;
import com.zzyl.common.utils.CodeGenerator;
import com.zzyl.common.utils.DateUtils;
import com.zzyl.common.utils.bean.BeanUtils;
import com.zzyl.nursing.domain.*;
import com.zzyl.nursing.dto.CheckInApplyDto;
import com.zzyl.nursing.dto.CheckInConfigDto;
import com.zzyl.nursing.dto.CheckInContractDto;
import com.zzyl.nursing.dto.CheckInElderDto;
import com.zzyl.nursing.mapper.*;
import com.zzyl.nursing.vo.CheckInConfigVo;
import com.zzyl.nursing.vo.CheckInDetailVo;
import com.zzyl.nursing.vo.CheckInElderVo;
import com.zzyl.nursing.vo.ElderFamilyVo;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.zzyl.nursing.service.ICheckInService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

/**
 * 入住Service业务层处理
 *
 * @author hinana
 * @date 2026-07-09
 */
@Service
public class CheckInServiceImpl extends ServiceImpl<CheckInMapper, CheckIn> implements ICheckInService
{
    @Autowired
    private CheckInMapper checkInMapper;
    @Autowired
    private ElderMapper elderMapper;
    @Autowired
    private ContractMapper contractMapper;
    @Autowired
    private CheckInConfigMapper checkInConfigMapper;
    @Autowired
    private BedMapper bedMapper;


    /**
     * 查询入住
     *
     * @param id 入住主键
     * @return 入住
     */
    @Override
    public CheckIn selectCheckInById(Long id)
    {
                return getById(id);
    }

    /**
     * 查询入住列表
     *
     * @param checkIn 入住
     * @return 入住
     */
    @Override
    public List<CheckIn> selectCheckInList(CheckIn checkIn)
    {
        return checkInMapper.selectCheckInList(checkIn);
    }

    /**
     * 新增入住
     *
     * @param checkIn 入住
     * @return 结果
     */
    @Override
    public int insertCheckIn(CheckIn checkIn)
    {
                checkIn.setCreateTime(DateUtils.getNowDate());
                        return save(checkIn) ? 1 : 0;
    }

    /**
     * 修改入住
     *
     * @param checkIn 入住
     * @return 结果
     */
    @Override
    public int updateCheckIn(CheckIn checkIn)
    {
                checkIn.setUpdateTime(DateUtils.getNowDate());
                return updateById(checkIn) ? 1 : 0;
    }

    /**
     * 批量删除入住
     *
     * @param ids 需要删除的入住主键
     * @return 结果
     */
    @Override
    public int deleteCheckInByIds(Long[] ids)
    {
                return removeByIds(Arrays.asList(ids)) ? 1 : 0;
    }

    /**
     * 删除入住信息
     *
     * @param id 入住主键
     * @return 结果
     */
    @Override
    public int deleteCheckInById(Long id)
    {
                return removeById(id) ? 1 : 0;
    }

    /**
     * 申请入驻
     * @param checkInApplyDto 发出申请入住接收的总接收DTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void apply(CheckInApplyDto checkInApplyDto) {
        CheckInElderDto elderDto = checkInApplyDto.getCheckInElderDto();
        CheckInConfigDto configDto = checkInApplyDto.getCheckInConfigDto();


        // 1. 判断老人是否已经入住
        Elder elder = elderMapper.selectActiveElderByIdCardNo(elderDto.getIdCardNo());
        if (ObjectUtils.isNotEmpty(elder)) {
            throw new BaseException("老人已入住");
        }
        // 2. 查询床位
        Bed bed = bedMapper.selectBedById(configDto.getBedId());
        if (ObjectUtils.isEmpty(bed)) {
            throw new BaseException("床位不存在");
        }
        // 3. 更新床位状态为已入住
        bed.setBedStatus(1);
        bed.setUpdateTime(DateUtils.getNowDate());
        bedMapper.updateBed(bed);
        // 4. 新增或更新老人
        elder = insertOrUpdateElder(bed, elderDto);
        // 5. 生成合同编号
        String contractNo = "HT" + CodeGenerator.generateContractNumber();
        // 6. 新增合同
        insertContract(contractNo, elder, checkInApplyDto);
        // 7. 新增入住信息
        CheckIn checkIn = insertCheckIn(elder, checkInApplyDto);
        // 8. 新增入住配置,注意这里需要入住ID，是在插入入住表后才会有的
        insertCheckInConfig(checkIn.getId(), checkInApplyDto);
    }

    private Elder insertOrUpdateElder(Bed bed, CheckInElderDto elderDto) {
        //新建老人对象，用来接收elderdto，并且补充elderdto没有的床编号、ID、状态
        Elder elder = new Elder();
        BeanUtils.copyProperties(elderDto, elder);
        elder.setBedNumber(bed.getBedNumber());
        elder.setBedId(bed.getId());
        elder.setStatus(1);

        // 查询身份证号相同、但状态不是“入住中”的老人（原本曾经住过）
        Elder elderDb = elderMapper.selectNotActiveElderByIdCardNo(elderDto.getIdCardNo());

        if (ObjectUtils.isNotEmpty(elderDb)) {
            // 注意：修改原本的elder时要根据elderDb设置 id，否则 update 不知道改哪条
            elder.setId(elderDb.getId());
            elder.setUpdateTime(new Date());
            elderMapper.updateElder(elder);
        } else {
            elder.setCreateTime(new Date());
            elder.setUpdateTime(new Date());
            elderMapper.insertElder(elder);
        }

        return elder;
    }

    private void insertContract(String contractNo, Elder elder, CheckInApplyDto checkInApplyDto) {
        //新建合同对象，用来接收contractdto,并且补充合同号，以及根据上面的老人信息，补充老人ID、老人姓名
        Contract contract = new Contract();
        BeanUtils.copyProperties(checkInApplyDto.getCheckInContractDto(), contract);

        contract.setContractNumber(contractNo);
        contract.setElderId(elder.getId());
        contract.setElderName(elder.getName());

        LocalDateTime checkInStartTime = checkInApplyDto.getCheckInConfigDto().getStartDate();
        LocalDateTime checkInEndTime = checkInApplyDto.getCheckInConfigDto().getEndDate();

        //根据入住开始时间是否晚于当前时间判断合同是否生效
        Integer status = checkInStartTime.isAfter(LocalDateTime.now()) ? 0 : 1;
        contract.setStatus(status);
        //将入住开始时间结束时间当成合同的开始结束时间
        contract.setStartDate(checkInStartTime);
        contract.setEndDate(checkInEndTime);

        contract.setUpdateTime(DateUtils.getNowDate());
        contract.setCreateTime(DateUtils.getNowDate());

        contractMapper.insertContract(contract);
    }

    private CheckIn insertCheckIn(Elder elder, CheckInApplyDto checkInApplyDto) {
         //新建checkin对象，用来接收elder与各个dto里面的常用信息
        CheckIn checkIn = new CheckIn();

        checkIn.setElderId(elder.getId());
        checkIn.setElderName(elder.getName());
        checkIn.setIdCardNo(elder.getIdCardNo());
        checkIn.setNursingLevelName(checkInApplyDto.getCheckInConfigDto().getNursingLevelName());
        checkIn.setStartDate(checkInApplyDto.getCheckInConfigDto().getStartDate());
        checkIn.setEndDate(checkInApplyDto.getCheckInConfigDto().getEndDate());
        checkIn.setBedNumber(elder.getBedNumber());
        //把前端传过来的家属信息以JSON形式存到checkin表的remark字段
        checkIn.setRemark(JSON.toJSONString(checkInApplyDto.getElderFamilyDtoList()));
        checkIn.setStatus(0);

        checkIn.setUpdateTime(DateUtils.getNowDate());
        checkIn.setCreateTime(DateUtils.getNowDate());

        checkInMapper.insertCheckIn(checkIn);

        return checkIn;
    }

    private void insertCheckInConfig(Long checkInId, CheckInApplyDto checkInApplyDto) {
        CheckInConfig checkInConfig = new CheckInConfig();
        BeanUtils.copyProperties(checkInApplyDto.getCheckInConfigDto(), checkInConfig);
       //插入checkinconfig表前需要获取checkin的id
        checkInConfig.setCheckInId(checkInId);

        checkInConfig.setCreateTime(DateUtils.getNowDate());
        checkInConfig.setUpdateTime(DateUtils.getNowDate());
        checkInConfigMapper.insertCheckInConfig(checkInConfig);
    }

    /**
     * 查询入住详细信息回显
     * @param id 入住主键
     * @return
     */
    @Override
    public CheckInDetailVo detail(Long id) {
        //1.新建CheckInDetailVo，用来接收总结果对象
        CheckInDetailVo checkInDetailVo=new CheckInDetailVo();
        //2.设置入住配置信息（从checkin和checkinconfig表里提取信息，放到CheckInConfigVo中，对应返回给前端的入住配置那一区域)
        CheckInConfigVo checkInConfigVo=new CheckInConfigVo();
        CheckIn checkIn = checkInMapper.selectCheckInById(id);
        BeanUtils.copyProperties(checkIn,checkInConfigVo);
        CheckInConfig checkInConfig = checkInConfigMapper.selectCheckInConfigByCheckInId(id);
        BeanUtils.copyProperties(checkInConfig,checkInConfigVo);
        checkInDetailVo.setCheckInConfigVo(checkInConfigVo);

        //3.设置老人信息
        CheckInElderVo checkInElderVo=new CheckInElderVo();
        Long elderId = checkIn.getElderId();
        Elder elder = elderMapper.selectElderById(elderId);
        BeanUtils.copyProperties(elder,checkInElderVo);
        //设置年龄
        checkInElderVo.setAge(IdcardUtil.getAgeByIdCard(elder.getIdCardNo()));
        checkInDetailVo.setCheckInElderVo(checkInElderVo);

        //4.设置签约合同信息
        Contract contract = contractMapper.selectContractByElderId(elderId);
        checkInDetailVo.setContract(contract);

        //5.设置家属信息
        String remark = checkIn.getRemark();
        List<ElderFamilyVo> elderFamilyVos = JSON.parseArray(remark, ElderFamilyVo.class);
        checkInDetailVo.setElderFamilyVoList(elderFamilyVos);

        return checkInDetailVo;




    }
}