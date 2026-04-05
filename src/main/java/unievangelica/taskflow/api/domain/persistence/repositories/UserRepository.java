package unievangelica.taskflow.api.domain.persistence.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;
import unievangelica.taskflow.api.domain.persistence.entities.UserEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    // atualização dos repositories parando de usar o JPA para gerenciar as consultas
    // e agora utilizando o JPQL para consultar diretamente matendo uma rastreabilidade maior de quais consultas são feitas
    // os métodos de insert ainda são gerenciados pelo JPA por meio da adição inteira das entidades
    @Query("SELECT COUNT (u) > 0 FROM User u WHERE u.email = :email")
    boolean existsByEmail(@Param("email") String email);

    @Query("SELECT u FROM User u WHERE u.email = :email")
    UserDetails findByEmail(@Param("email")String email);

    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<UserEntity> findById(@Param("id") Long id);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.id = :id")
    boolean existsById(@Param("id") Long id);

    @Query("SELECT u FROM User u")
    List<UserEntity> findAll();

    // esses são os métodos modify nos update e delete para alterar apenas alguns campos do db assim
    // desacoplando a responsabilidade do JPA de sempre salvar uma entidade inteira ao fazer um update
    @Modifying
    @Query("UPDATE User u SET u.nome = :nome, u.email = :email WHERE u.id = :id")
    void updateUser(@Param("id") Long id, @Param("nome") String nome, @Param("email") String email);

    @Modifying
    @Query("UPDATE User u SET u.senha = :senha WHERE u.id = :id")
    void updateUserPassword(@Param("id") Long id, @Param("senha") String senha);

    @Modifying
    @Query("DELETE FROM User u WHERE u.id = :id")
    void deleteById(@Param("id") Long id);
}
