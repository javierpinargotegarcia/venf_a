package com.inventario.ui;

import com.inventario.Aplicacion;
import com.inventario.AppContexto;
import com.inventario.excepcion.ExcepcionNegocio;
import com.inventario.modelo.Usuario;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/** Pantalla de acceso al sistema. */
public class ControladorLogin {

    @FXML
    private TextField campoUsuario;
    @FXML
    private PasswordField campoClave;
    @FXML
    private Label etiquetaError;
    @FXML
    private Label etiquetaModo;

    @FXML
    private void initialize() {
        boolean mysql = AppContexto.config().esModoMysql();
        etiquetaModo.setText(mysql
                ? "Almacenamiento: base de datos MySQL"
                : "Almacenamiento: archivos JSON en la carpeta '"
                + AppContexto.config().getCarpetaDatos() + "/'");
        campoUsuario.requestFocus();
    }

    @FXML
    private void entrar() {
        try {
            Usuario usuario = AppContexto.autenticacion()
                    .autenticar(campoUsuario.getText(), campoClave.getText());
            etiquetaError.setText("");
            campoClave.clear();
            Aplicacion.mostrarPrincipal();
            Aplicacion.estado("Sesion iniciada como " + usuario.getUsuario()
                    + " (" + usuario.getRol() + ")");
        } catch (ExcepcionNegocio e) {
            etiquetaError.setText(e.getMessage());
            campoClave.clear();
            campoClave.requestFocus();
        } catch (Exception e) {
            Alertas.excepcion("No se pudo iniciar sesion", e);
        }
    }
}
