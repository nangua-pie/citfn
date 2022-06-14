package com.forum.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.forum.file.entity.FileRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Uploaded file access.
 */
@Mapper
public interface FileRecordMapper extends BaseMapper<FileRecord> {

    @Select("SELECT * FROM sys_file WHERE object_key = #{objectKey}")
    FileRecord selectByObjectKey(@Param("objectKey") String objectKey);

    @Select("SELECT * FROM sys_file WHERE uploader_id = #{uploaderId} ORDER BY create_time DESC LIMIT #{limit}")
    List<FileRecord> selectRecentByUploader(@Param("uploaderId") Long uploaderId,
                                            @Param("limit") Integer limit);
}
