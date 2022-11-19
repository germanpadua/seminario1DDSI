package com.bandadelpatio;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Savepoint;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Scanner;
import java.util.ArrayList;

public class App {
    public static void main(String[] args) {
        try {
            // Se carga el driver JDBC
            DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());
            // nombre del servidor
            String nombre_servidor = "oracle0.ugr.es";
            // numero del puerto
            String numero_puerto = "1521";
            // SID
            String sid = "practbd.oracle0.ugr.es";
            // URL "jdbc:oracle:thin:@nombreServidor:numeroPuerto:SID"
            String url = "jdbc:oracle:thin:@" + nombre_servidor + ":" + numero_puerto + "/" + sid;

            // Nombre usuario y password
            String usuario = "x7147851";
            String password = "x7147851";

            // Obtiene la conexion
            Connection conexion = DriverManager.getConnection(url, usuario, password);
            conexion.setAutoCommit(false);

            Scanner entrada = new Scanner(System.in);
            int opcion;
            Statement sentencia;
            ResultSet rs;
            boolean salir = false;

            while(!salir){
                System.out.println("¿Qué consulta desea realizar?");
                System.out.println("1. Borrado y nueva creación de las tablas e inserción de 10 tuplas predefinidas");
                System.out.println("2. Dar de alta nuevo pedido");
                System.out.println("3. Mostrar el contenido de las tablas de la BD");
                System.out.println("4. Salir del programa y cerrar la conexión con la BD");
                System.out.println("Introduzca el número de la opción que desea realizar: ");

                while (true) {
                    try {
                        opcion = entrada.nextInt();
                        break;
                    } catch (Exception e) {
                        System.out.println("Error: Introduzca un número");
                        entrada.nextLine();
                    }
                }

                switch (opcion) {
                    case 1:
                        sentencia = conexion.createStatement();

                        // Eliminamos las tablas si están creadas
                        rs = sentencia.executeQuery("SELECT COUNT(table_name) FROM user_tables");
                        rs.next();
                        if (rs.getInt(1) != 0) {
                            sentencia.executeQuery("DROP TABLE detalle_pedido");
                            sentencia.executeQuery("DROP TABLE pedido");
                            sentencia.executeQuery("DROP TABLE stock");
                        }

                        // Se realiza la consulta. Los resultados se guardan en el ResultSet rs
                        sentencia.executeQuery("CREATE TABLE stock (cproducto VARCHAR2(10) CONSTRAINT stock_clave_primaria PRIMARY KEY, cantidad INT)");
                        sentencia.executeQuery("CREATE TABLE pedido (cpedido VARCHAR2(10) CONSTRAINT pedido_clave_primaria PRIMARY KEY,ccliente VARCHAR2(5), fecha_pedido DATE)");
                        sentencia.executeQuery("CREATE TABLE detalle_pedido (cpedido VARCHAR2(10) CONSTRAINT cpedido_clave_externa_pedido REFERENCES pedido(cpedido),cproducto VARCHAR2(10) CONSTRAINT cproducto_clave_primaria REFERENCES stock(cproducto), cantidad INT, CONSTRAINT detalle_clave_primaria PRIMARY KEY (cpedido, cproducto))");
                        sentencia.executeQuery("INSERT INTO stock VALUES('pro1', 100)");
                        sentencia.executeQuery("INSERT INTO stock VALUES('pro2', 150)");
                        sentencia.executeQuery("INSERT INTO stock VALUES('pro3', 200)");
                        sentencia.executeQuery("INSERT INTO stock VALUES('pro4', 50)");
                        sentencia.executeQuery("INSERT INTO stock VALUES('pro5', 10)");
                        sentencia.executeQuery("INSERT INTO stock VALUES('pro6', 75)");
                        sentencia.executeQuery("INSERT INTO stock VALUES('pro7', 500)");
                        sentencia.executeQuery("INSERT INTO stock VALUES('pro8', 250)");
                        sentencia.executeQuery("INSERT INTO stock VALUES('pro9', 100)");
                        sentencia.executeQuery("INSERT INTO stock VALUES('pro10', 80)");

                        // Guardamos los resultados
                        sentencia.executeQuery("COMMIT");
                        System.out.println("Datos por defecto restaurados.");

                        // Se cierra el Statement
                        sentencia.close();

                        break;
                    case 2:
                        sentencia = conexion.createStatement();
                        Savepoint savepoint1 = conexion.setSavepoint(); // Savepoint cuando no se ha creado el pedido
                        //sentencia.executeQuery("SAVEPOINT <savepoint1>");

                        String codpedido, ccliente, fecha="";
                        // Leemos el código del pedido sin excedernos de 10 caracteres
                        do {
                            System.out.print("Introduce el código del pedido (10 char máx): ");
                            codpedido = entrada.next();
                        } while (codpedido.length() > 10);
                        System.out.println("codpedido: " + codpedido);
                        // Leemos el código del cliente sin excedernos de 5 caracteres
                        do {
                            System.out.print("Introduce el código del cliente (5 char máx): ");
                            ccliente = entrada.next();
                        } while (ccliente.length() > 5);
                        System.out.println("ccliente: " + ccliente);

                        // Comprobamos que el formato de la fecha sea correcto
                        boolean fecha_incorrecta = true;
                        while (fecha_incorrecta){
                            fecha_incorrecta = false;
                            System.out.print("Introduce la fecha (formato yyyy-MM-dd): ");
                            fecha = entrada.next();
                            System.out.println("fecha: " + fecha);

                            DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                            sdf.setLenient(false); // lenient = tolerante (lo ponemos a false para que dé error si el formato no es correcto o si no es válida la fecha)
                            try{
                                sdf.parse(fecha);
                            } catch (Exception e){
                                System.out.println("Error: Formato de fecha incorrecto o fecha inválida");
                                fecha_incorrecta = true;
                            }
                        }

                        // Hacemos la consulta con control de excepciones por si el código del pedido ya existiese
                        boolean consulta_valida = false;
                        do{
                           try{
                               sentencia.executeQuery("INSERT INTO pedido VALUES('" + codpedido + "', '" + ccliente + "', to_date('" + fecha + "','yyyy-mm-dd'))");
                               consulta_valida = true;
                           } catch (Exception e){
                               System.out.println("El código del pedido que ha introducido ya existe.");
                               System.out.print("Por favor, introduzca otro: ");
                               codpedido = entrada.next();
                           }
                        } while (!consulta_valida);

                        Savepoint savepoint2 = conexion.setSavepoint(); // Savepoint antes de añadir ningún detalle

                        boolean continuar = true;

                        while(continuar){
                            System.out.println("--- ¿Qué opción desea realizar? ----");
                            System.out.println("1. Añadir detalle de producto");
                            System.out.println("2. Eliminar todos los detalles de producto");
                            System.out.println("3. Cancelar el pedido");
                            System.out.println("4. Finalizar pedido");
                            System.out.println("Introduzca el número de la opción que desea realizar: ");

                            while (true) {
                                try {
                                    opcion = entrada.nextInt();
                                    break;
                                } catch (Exception e) {
                                    System.out.println("Error: Introduzca un número");
                                    entrada.nextLine();
                                }
                            }

                            switch(opcion){
                                case 1:
                                    String codpro;
                                    System.out.print("Introducir el código del producto: ");
                                    codpro = entrada.next();

                                    // Vemos si existe en nuestra base de datos tal producto
                                    rs = sentencia.executeQuery("SELECT COUNT(*) FROM stock WHERE cproducto = '" + codpro + "'");
                                    rs.next();
                                    int numproductos = rs.getInt(1);

                                    if (numproductos == 0){
                                        System.out.println("El código de producto introducido no existe");
                                    } else{
                                        int cant;

                                        // Recuperamos la cantidad que tenemos disponible
                                        rs = sentencia.executeQuery("SELECT cantidad FROM stock WHERE cproducto = '" + codpro + "'");
                                        rs.next();
                                        int cantidad_actual = rs.getInt(1);

                                        // Leemos la cantidad deseada y verificamos que es correcta
                                        if (cantidad_actual == 0){
                                            System.out.println("No hay cantidad disponble de este producto");
                                        }
                                        else {
                                            do {
                                                System.out.print("Introducir la cantidad deseada del producto (menor que la disponible y positiva): ");
                                                cant = entrada.nextInt();
                                            } while (cant <= 0 || cantidad_actual < cant);

                                            try{
                                                sentencia.executeQuery("INSERT INTO detalle_pedido VALUES('" + codpedido + "', '" + codpro + "', '" + cant + "')");
                                                sentencia.executeQuery("UPDATE stock SET cantidad = cantidad - " + cant + " WHERE cproducto = '" + codpro +"'");
                                            } catch (Exception e){
                                                System.out.println("No se pudo insertar... La compra de este producto ya había sido añadida al pedido");
                                            }
                                        }
                                    }

                                    // Imprimimos el contenido de la base de datos
                                    imprimirEstadoBD(conexion);

                                    break;

                                case 2:
                                    conexion.rollback(savepoint2);

                                    // Imprimimos el contenido de la base de datos
                                    imprimirEstadoBD(conexion);

                                    break;

                                case 3:
                                    conexion.rollback(savepoint1);
                                    sentencia.executeQuery("COMMIT");
                                    continuar = false;

                                    // Imprimimos el contenido de la base de datos
                                    imprimirEstadoBD(conexion);

                                    break;

                                case 4:
                                    sentencia.executeQuery("COMMIT");
                                    continuar = false;
                                    break;
                            };
                        }
                        break;
                    case 3:
                        imprimirEstadoBD(conexion);
                        break;
                    case 4:
                        sentencia = conexion.createStatement();
                        sentencia.executeQuery("COMMIT");
                        sentencia.close();
                        conexion.close();
                        salir = true;
                        break;
                    default:
                        System.out.println("Opción no válida");
                        break;
                };

            }

            entrada.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void imprimirEstadoBD(Connection conexion) throws java.sql.SQLException {
        Statement sentencia;
        ResultSet rs, rs2;
        sentencia = conexion.createStatement();
        rs = sentencia.executeQuery("SELECT table_name FROM user_tables");
        ArrayList<String> nombre_tablas = new ArrayList<String>();

        // Conseguimos el nombre de las tablas
        while(rs.next()){
            nombre_tablas.add(rs.getString("table_name"));
        }

        // Iteramos para cada tabla
        for (int indice = 0; indice < nombre_tablas.size(); indice++){
            String nombre_tabla = nombre_tablas.get(indice);

            System.out.println("----------------------------------------------------------");
            System.out.println("Imprimiendo información de: " + nombre_tabla.toUpperCase());
            System.out.println("----------------------------------------------------------");

            rs2 = sentencia.executeQuery("SELECT * FROM " + nombre_tabla);
            ResultSetMetaData rsmd = rs2.getMetaData();
            int numero_columnas = rsmd.getColumnCount();

            // Imprimimos las columnas
            for (int i = 1; i <= numero_columnas; i++) {
                if (i > 1) System.out.print(", ");
                System.out.print(rsmd.getColumnName(i).toUpperCase());
            }
            System.out.println();

            // Vamos iterando por las filas e imprimiéndolas
            while (rs2.next()){
                for (int i = 1; i <= numero_columnas; i++){
                    if (i > 1) System.out.print(", ");
                    System.out.print(rs2.getString(i));
                }
                System.out.println();
            }
            System.out.println();
            rs2.close();
        }

        rs.close();
        sentencia.close();
        nombre_tablas.clear();
    }
}