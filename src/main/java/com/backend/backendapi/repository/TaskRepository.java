package com.backend.backendapi.repository;

import com.backend.backendapi.domain.Task;
import com.backend.backendapi.dto.task.TitleCountRow;
import com.backend.backendapi.dto.task.TitleUserRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findAllByOrderByIdAsc();

    List<Task> findByUserOrderByIdAsc(String user);

    boolean existsByUser(String user);

    Optional<Task> findByIdAndUser(Long id, String user);

    @Query("""
            select new com.backend.backendapi.dto.task.TitleCountRow(t.title, count(distinct t.user))
            from Task t
            where t.user <> :user
              and t.title in (select t2.title from Task t2 where t2.user = :user)
            group by t.title
            order by t.title
            """)
    List<TitleCountRow> countOtherUsersWithSameTitles(@Param("user") String user);

    @Query("""
            select new com.backend.backendapi.dto.task.TitleUserRow(t.title, t.user)
            from Task t
            where t.user <> :user
              and t.title in (select t2.title from Task t2 where t2.user = :user)
            group by t.title, t.user
            order by t.title, t.user
            """)
    List<TitleUserRow> findOtherUsersWithSameTitles(@Param("user") String user);
}
