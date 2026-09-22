package spring4.tuto.search.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import spring4.tuto.common.dto.ApiResponse;
import spring4.tuto.search.service.SearchService;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public ResponseEntity<ApiResponse<SearchService.GlobalSearchResult>> search(@RequestParam("q") String query) {
        SearchService.GlobalSearchResult result = searchService.search(query);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
