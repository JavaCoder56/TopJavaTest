package com.game.service;

import com.game.entity.Player;
import com.game.entity.Profession;
import com.game.entity.Race;
import com.game.exception.BadRequestException;
import com.game.exception.NotFoundException;
import com.game.repository.PlayerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;

@Service
public class PlayerService {
    private PlayerRepository playerRepository;

    public PlayerService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    public void checkId(Long id) {
        if(id<=0) {
            throw new BadRequestException("Invalid ID");
        }
    }
    public void checkName(String name) {
        if(name==null||name.length()>12) {
            throw new BadRequestException("Invalid name");
        }
    }
    public void checkTitle(String title) {
        if(title==null||title.isEmpty()||title.length()>30) {
            throw new BadRequestException("Invalid title");
        }
    }
    public void checkRace(Race race) {
        if(race==null) {
            throw new BadRequestException("Invalid race");
        }
    }
    public void checkProfession (Profession profession) {
        if(profession==null) {
            throw new BadRequestException("Invalid profession");
        }
    }
    public void checkBirthday(Date birthday) {
        if(birthday==null)
            throw new BadRequestException("Birthday is invalid");
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(birthday.getTime());
        if (calendar.get(Calendar.YEAR) < 2000L || calendar.get(Calendar.YEAR) > 3000L)
            throw new BadRequestException("Birthday is out of bounds");
    }
    public void checkExperience(Integer experience){
        if (experience==null||experience<0||experience>10000000) {
            throw new BadRequestException("Invalid experience");
        }
    }

    public Integer calculatePlayerLevel(Integer experience) {
        return (int)(Math.sqrt(2500+200*experience)-50)/100;
    }

    public Integer calculateExpForNextLvl(Integer level, Integer experience) {
        return 50*(level+1)*(level+2)-experience;
    }

    public Page<Player> findAllPlayers(Specification<Player> specification, Pageable pageable) {
        return playerRepository.findAll(specification, pageable);
    }

    public Long getCountPlayers(Specification<Player> specification) {
        return playerRepository.count(specification);
    }

    public Player createPlayer (Player player) {
        checkName(player.getName());
        checkTitle(player.getTitle());
        checkRace(player.getRace());
        checkProfession(player.getProfession());
        checkBirthday(player.getBirthday());
        checkExperience(player.getExperience());
        if (player.getBanned()==null) {
            player.setBanned(false);
        }
        player.setLevel(calculatePlayerLevel(player.getExperience()));
        player.setUntilNextLevel(calculateExpForNextLvl(player.getLevel(),player.getExperience()));
        return playerRepository.saveAndFlush(player);
    }

    public Player getPlayerById(Long id) {
        checkId(id);
        return playerRepository.findById(id).orElseThrow(()-> new NotFoundException("No such player"));
    }

    public Player updatePlayer(Long id, Player player) {
        Player playerForUpdate = getPlayerById(id);
        if(player.getName()!=null) {
            checkName(player.getName());
            playerForUpdate.setName(player.getName());
        }
        if(player.getTitle()!=null) {
            checkTitle(player.getTitle());
            playerForUpdate.setName(player.getName());
        }
        if(player.getRace()!=null) {
            checkRace(player.getRace());
            playerForUpdate.setRace(player.getRace());
        }
        if(player.getProfession()!=null) {
            checkProfession(player.getProfession());
            playerForUpdate.setProfession(player.getProfession());
        }
        if(player.getBirthday()!=null) {
            checkBirthday(player.getBirthday());
            playerForUpdate.setBirthday(player.getBirthday());
        }
        if(player.getBanned()!=null) {
            playerForUpdate.setBanned(player.getBanned());
        }
        if (player.getExperience() != null) {
            checkExperience(player.getExperience());
            playerForUpdate.setExperience(player.getExperience());
        }
        playerForUpdate.setLevel(calculatePlayerLevel(playerForUpdate.getExperience()));
        playerForUpdate.setUntilNextLevel(calculateExpForNextLvl(playerForUpdate.getLevel(),
                playerForUpdate.getExperience()));
        return playerRepository.save(playerForUpdate);
    }
    public void deletePlayer(Long id) {
        Player playerForDelete = getPlayerById(id);
        playerRepository.delete(playerForDelete);
    }

    public Specification<Player> filterByName(String name) {
        return (root,query,cb)->name==null?null:cb.like(root.get("name"),"%"+name+"%");
    }

    public Specification<Player> filterByTitle(String title) {
        return (root,query,cb)->title==null?null:cb.like(root.get("title"),"%"+title+"%");
    }

    public Specification<Player> filterByRace(Race race) {
        return (root,query,cb)->race==null?null:cb.equal(root.get("race"),race);
    }

    public Specification<Player> filterByProfession(Profession profession) {
        return (root,query,cb)->profession==null?null:cb.equal(root.get("profession"),profession);
    }

    public Specification<Player> filterByExperience(Integer min,Integer max) {
        return (root,query,cb)->{
            if (min==null && max==null) return null;
            if (min==null) return cb.lessThanOrEqualTo(root.get("experience"), max);
            if (max==null) return cb.greaterThanOrEqualTo(root.get("experience"), min);
            return cb.between(root.get("experience"), min, max);
        };
    }

    public Specification<Player> filterByLevel(Integer min,Integer max) {
        return (root,query,cb)->{
            if (min==null && max==null) return null;
            if (min==null) return cb.lessThanOrEqualTo(root.get("level"), max);
            if (max==null) return cb.greaterThanOrEqualTo(root.get("level"), min);
            return cb.between(root.get("level"), min, max);
        };
    }

    public Specification<Player> filterByUntilNextLevel(Integer min, Integer max) {
        return (root,query,cb)->{
            if (min==null && max==null) return null;
            if (min==null) return cb.lessThanOrEqualTo(root.get("untilNextLevel"), max);
            if (max==null) return cb.greaterThanOrEqualTo(root.get("untilNextLevel"), min);
            return cb.between(root.get("untilNextLevel"), min, max);
        };
    }

    public Specification<Player> filterByBirthday(Long after, Long before) {
        return (root,query,cb)->{
            if (after==null && before==null) return null;
            if (after==null) return cb.lessThanOrEqualTo(root.get("birthday"), new Date(before));
            if (before==null) return cb.greaterThanOrEqualTo(root.get("birthday"), new Date(after));
            return cb.between(root.get("birthday"), new Date(after), new Date(before));
        };
    }

    public Specification<Player> filterByBanned(Boolean isBanned) {
        return (root,query,cb)->{
            if (isBanned==null) return null;
            if (isBanned) return cb.isTrue(root.get("banned"));
            return cb.isFalse(root.get("banned"));
        };
    }
}
