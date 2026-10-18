package me.kimmukwan.company.company.repository;

import me.kimmukwan.company.company.domain.Company;
import me.kimmukwan.company.login.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    List<Company> findByUser(User user);
}
