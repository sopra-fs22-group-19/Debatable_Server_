package ch.uzh.ifi.hase.soprafs22.controller;

import ch.uzh.ifi.hase.soprafs22.constant.DebateSide;
import ch.uzh.ifi.hase.soprafs22.constant.DebateState;
import ch.uzh.ifi.hase.soprafs22.entity.DebateRoom;
import ch.uzh.ifi.hase.soprafs22.entity.DebateTopic;
import ch.uzh.ifi.hase.soprafs22.rest.dto.DebateRoomGetDTO;
import ch.uzh.ifi.hase.soprafs22.rest.dto.DebateRoomPostDTO;
import ch.uzh.ifi.hase.soprafs22.rest.dto.DebateTopicGetDTO;
import ch.uzh.ifi.hase.soprafs22.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs22.service.DebateService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@RestController
public class DebateController {

    private final DebateService debateService;

    DebateController(DebateService debateService) {
        this.debateService = debateService;
    }

    @PostMapping("/debates/rooms")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public DebateRoomGetDTO createDebateRoom(@RequestBody DebateRoomPostDTO debateRoomPostDTO) {
        // convert API user to internal representation
        DebateRoom inputDebateRoom = DTOMapper.INSTANCE.convertDebateRoomPostDTOtoEntity(debateRoomPostDTO);

        // Set the state of the Debate Room depending on the side of the user that created the room
        if (debateRoomPostDTO.getSide().equalsIgnoreCase(DebateSide.FOR.name()))
            inputDebateRoom.setDebateRoomStatus(DebateState.ONE_USER_FOR);
        else if (debateRoomPostDTO.getSide().equalsIgnoreCase(DebateSide.AGAINST.name()))
            inputDebateRoom.setDebateRoomStatus(DebateState.ONE_USER_AGAINST);
        else{
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Error: reason <the 'side' field must be either 'FOR' or 'AGAINST'>");
        }

        // Create the debate room in the DB
        DebateRoom createdDebateRoom = debateService.createDebateRoom(inputDebateRoom, debateRoomPostDTO);

        // convert internal representation of user back to API
        return DTOMapper.INSTANCE.convertEntityToDebateRoomGetDTO(createdDebateRoom);
    }



    @GetMapping("/debates/{userId}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<DebateTopicGetDTO> getTopicByUser(@PathVariable Long userId){

        List<DebateTopicGetDTO> debateGetDTOs = new ArrayList<>();
        List<DebateTopic> debateTopics = debateService.getDebateTopicByUserId(userId);

        for (DebateTopic debateTopic : debateTopics) {
            debateGetDTOs.add(DTOMapper.INSTANCE.convertEntityToDebateGetDTO(debateTopic));
        }

        return debateGetDTOs;
    }

}