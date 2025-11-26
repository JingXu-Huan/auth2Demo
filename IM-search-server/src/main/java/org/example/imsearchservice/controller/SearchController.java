package org.example.imsearchservice.controller;

import org.example.imsearchservice.domain.ImMessageDoc;
import org.example.imsearchservice.service.SearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 对外提供搜索能力的 REST 控制器（简化版）。
 */
@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/messages")
    public List<ImMessageDoc> searchMessages(@RequestParam("q") String query,
                                             @RequestParam(value = "channelIds", required = false) String channelIds) {
        List<Long> channelIdList = new ArrayList<>();
        if (channelIds != null && !channelIds.isEmpty()) {
            String[] parts = channelIds.split(",");
            for (String part : parts) {
                try {
                    channelIdList.add(Long.parseLong(part.trim()));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return searchService.searchMessages(query, channelIdList);
    }
}
