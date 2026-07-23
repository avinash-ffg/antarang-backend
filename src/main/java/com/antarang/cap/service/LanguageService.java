package com.antarang.cap.service;

import com.antarang.cap.domain.entity.Language;
import com.antarang.cap.dto.response.LanguageResponse;
import com.antarang.cap.repository.LanguageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LanguageService {

    private final LanguageRepository languageRepository;

    public LanguageService(LanguageRepository languageRepository) {
        this.languageRepository = languageRepository;
    }

    @Transactional(readOnly = true)
    public List<LanguageResponse> listLanguages() {
        return languageRepository.findByIsDeletedFalse().stream()
                .map(this::toResponse)
                .toList();
    }

    private LanguageResponse toResponse(Language language) {
        return new LanguageResponse(
                language.getId(),
                language.getCode(),
                language.getName(),
                language.getNativeName(),
                language.isDefault()
        );
    }
}
