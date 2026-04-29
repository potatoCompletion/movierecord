package com.my.movierecord.record.dto;

import com.my.movierecord.record.domain.WatchRecord;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
public class RecordPageDto {

    Page<WatchRecord> page;
    List<RecordListItem> items;

    public static RecordPageDto of(Page<WatchRecord> page,  List<RecordListItem> items) {
        RecordPageDto dto = new RecordPageDto();
        dto.page = page;
        dto.items = items;
        return dto;
    }

}
