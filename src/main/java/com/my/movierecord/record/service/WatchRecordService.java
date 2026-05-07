package com.my.movierecord.record.service;

import com.my.movierecord.auth.domain.User;
import com.my.movierecord.auth.repository.UserRepository;
import com.my.movierecord.movie.domain.Content;
import com.my.movierecord.movie.service.ContentService;
import com.my.movierecord.record.domain.WatchRecord;
import com.my.movierecord.record.dto.RecordListItem;
import com.my.movierecord.record.dto.RecordPageDto;
import com.my.movierecord.record.repository.WatchRecordRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class WatchRecordService {

    private final WatchRecordRepository watchRecordRepository;
    private final ContentService contentService;
    private final UserRepository userRepository;

    public WatchRecordService(WatchRecordRepository watchRecordRepository,
                              ContentService contentService,
                              UserRepository userRepository) {
        this.watchRecordRepository = watchRecordRepository;
        this.contentService = contentService;
        this.userRepository = userRepository;
    }

    public RecordPageDto list(Pageable pageable) {
        Page<WatchRecord> page = watchRecordRepository.findAll(pageable);
        List<RecordListItem> items = page.map(RecordListItem::from).toList();
        return RecordPageDto.of(page, items);
    }

    public WatchRecord get(Long id) {
        return watchRecordRepository.findByIdWithFetch(id)
                .orElseThrow(() -> new EntityNotFoundException("영화를 찾을 수 없습니다. id=" + id));
    }

    @Transactional
    public WatchRecord create(WatchRecordSaveCommand command) {
        User user = userRepository.getReferenceById(command.userId());
        Content content = resolveContent(command);
        WatchRecord record = WatchRecord.builder()
                .title(command.title())
                .watchedDate(command.watchedDate())
                .oneLiner(command.oneLiner())
                .immersion(command.immersion())
                .story(command.story())
                .emotions(command.emotions())
                .goodPoints(command.goodPoints())
                .badPoints(command.badPoints())
                .taste(command.taste())
                .rating(command.rating())
                .user(user)
                .content(content)
                .build();
        return watchRecordRepository.save(record);
    }

    public RecordPageDto listByUser(Long userId, Pageable pageable) {
        Page<WatchRecord> page = watchRecordRepository.findByUserId(userId, pageable);
        List<RecordListItem> items = page.map(RecordListItem::from).toList();
        return RecordPageDto.of(page, items);
    }

    @Transactional
    public WatchRecord update(Long id, WatchRecordSaveCommand command) {
        WatchRecord record = get(id);
        Content content = resolveContent(command);
        record.update(
                command.title(),
                command.watchedDate(),
                command.oneLiner(),
                command.immersion(),
                command.story(),
                command.emotions(),
                command.goodPoints(),
                command.badPoints(),
                command.taste(),
                command.rating(),
                content
        );
        return record;
    }

    @Transactional
    public void delete(Long id) {
        WatchRecord record = get(id);
        watchRecordRepository.delete(record);
    }

    private Content resolveContent(WatchRecordSaveCommand command) {
        if (command.tmdbId() == null) {
            return null;
        }
        return contentService.findOrCreate(command.tmdbId(), command.mediaType(), command.posterPath());
    }
}
