package com.seniorapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seniorapp.dto.project.StoryPointDtos.SaveStoryPointsRequest;
import com.seniorapp.dto.project.StoryPointDtos.StoryPointEntry;
import com.seniorapp.dto.project.StoryPointDtos.StoryPointsListResponse;
import com.seniorapp.dto.project.StoryPointDtos.StoryPointsPayload;
import com.seniorapp.dto.project.StoryPointDtos.StudentStoryPointRowDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ensures story-point API DTOs serialize with the same Jackson setup as Spring MVC.
 */
class StoryPointDtosJacksonTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void storyPointsListResponse_serializes() throws Exception {
        StoryPointsPayload payload = new StoryPointsPayload();
        payload.setEditable(true);
        StudentStoryPointRowDto row = new StudentStoryPointRowDto();
        row.setStudentUserId(42L);
        row.setFullName("Ali Veli");
        row.setEmail("a@b.com");
        row.setMembershipRole("LEADER");
        row.setStoryPoints(5.5);
        payload.setRows(List.of(row));

        StoryPointsListResponse response = new StoryPointsListResponse("success", payload);
        String json = mapper.writeValueAsString(response);
        assertThat(json).contains("\"status\":\"success\"");
        assertThat(json).contains("\"editable\":true");
        assertThat(json).contains("\"rows\"");
        assertThat(json).contains("Ali Veli");
    }

    @Test
    void saveStoryPointsRequest_roundTrip() throws Exception {
        SaveStoryPointsRequest req = new SaveStoryPointsRequest();
        StoryPointEntry e = new StoryPointEntry();
        e.setStudentUserId(1L);
        e.setStoryPoints(3.0);
        req.setEntries(List.of(e));
        String json = mapper.writeValueAsString(req);
        SaveStoryPointsRequest back = mapper.readValue(json, SaveStoryPointsRequest.class);
        assertThat(back.getEntries()).hasSize(1);
        assertThat(back.getEntries().get(0).getStudentUserId()).isEqualTo(1L);
    }
}
