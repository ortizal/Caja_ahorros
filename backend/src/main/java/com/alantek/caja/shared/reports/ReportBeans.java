package com.alantek.caja.shared.reports;

public final class ReportBeans {
    private ReportBeans() {}

    public record SocioData(String codigo, String identificacion, String nombres,
                             String apellidos, String telefono, String email,
                             String estado, String fechaIngreso) {}

    public record CajaData(String cajero, String fechaApertura, String saldoInicial,
                            String totalIngresos, String totalEgresos,
                            String saldoFinal, String estado) {}

    public record CarteraData(String socio, String producto, String numeroCuota,
                               String fechaVencimiento, String capital, String cuotaTotal,
                               String mora, String totalPagar, String estado, String diasVencido) {}

    public record AhorroData(String codigo, String socio, String producto,
                              String saldo, String estado, String fechaApertura) {}

    public record AportacionData(String socio, String periodo, String monto,
                                  String estado, String pagado) {}

    public record CreditoData(String socio, String producto, String monto,
                               String tasa, String plazo, String cuotaMensual,
                               String estado, String fechaDesembolso) {}

    public record UsuarioData(String username, String nombreCompleto, String email,
                               String estado, String roles, String ultimoAcceso) {}
}
