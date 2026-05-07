package com.my.movierecord.record.service;

import com.my.movierecord.auth.domain.User;
import com.my.movierecord.auth.repository.UserRepository;
import com.my.movierecord.movie.service.ContentService;
import com.my.movierecord.record.domain.WatchRecord;
import com.my.movierecord.record.dto.RecordPageDto;
import com.my.movierecord.record.repository.WatchRecordRepository;
import com.my.movierecord.support.WatchRecordFixture;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class WatchRecordServiceTest {

    @Mock
    WatchRecordRepository watchRecordRepository;

    @Mock
    ContentService contentService;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    WatchRecordService watchRecordService;

    @Test
    void list_repository_위임() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<WatchRecord> page = new PageImpl<>(List.of(WatchRecordFixture.createWatchRecordWithId(1L)), pageable, 1);
        given(watchRecordRepository.findAll(pageable)).willReturn(page);

        RecordPageDto result = watchRecordService.list(pageable);

        assertThat(result.getPage()).isEqualTo(page);
        then(watchRecordRepository).should().findAll(pageable);
    }

    @Test
    void get_존재하는_id_반환() {
        WatchRecord record = WatchRecordFixture.createWatchRecordWithId(1L);
        given(watchRecordRepository.findByIdWithFetch(1L)).willReturn(Optional.of(record));

        WatchRecord result = watchRecordService.get(1L);

        assertThat(result.getTitle()).isEqualTo(record.getTitle());
    }

    @Test
    void get_없는_id_EntityNotFoundException() {
        given(watchRecordRepository.findByIdWithFetch(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> watchRecordService.get(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void create_save_호출_및_반환() {
        WatchRecordSaveCommand command = WatchRecordFixture.createCommand();
        WatchRecord record = WatchRecordFixture.createWatchRecordWithId(1L);
        given(userRepository.getReferenceById(1L)).willReturn(org.mockito.Mockito.mock(User.class));
        given(watchRecordRepository.save(any(WatchRecord.class))).willReturn(record);

        WatchRecord result = watchRecordService.create(command);

        then(watchRecordRepository).should().save(any(WatchRecord.class));
        assertThat(result).isEqualTo(record);
    }

    @Test
    void update_성공() {
        WatchRecord record = WatchRecordFixture.createWatchRecordWithId(1L);
        given(watchRecordRepository.findByIdWithFetch(1L)).willReturn(Optional.of(record));

        WatchRecord result = watchRecordService.update(1L, WatchRecordFixture.createCommand());

        assertThat(result.getTitle()).isEqualTo("테스트 영화");
    }

    @Test
    void delete_성공() {
        WatchRecord record = WatchRecordFixture.createWatchRecordWithId(1L);
        given(watchRecordRepository.findByIdWithFetch(1L)).willReturn(Optional.of(record));

        watchRecordService.delete(1L);

        then(watchRecordRepository).should().delete(record);
    }

    @Test
    void delete_없는_id_예외_전파() {
        given(watchRecordRepository.findByIdWithFetch(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> watchRecordService.delete(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
