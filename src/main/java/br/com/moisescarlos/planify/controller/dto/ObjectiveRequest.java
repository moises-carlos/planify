package br.com.moisescarlos.planify.controller.dto;
import jakarta.validation.constraints.NotBlank;

public record ObjectiveRequest(@NotBlank(message = "O texto do objetivo não pode estar em branco.") String text){ }
