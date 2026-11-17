package com.pccheck.agent.collector;

import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;
import oshi.util.platform.windows.WmiQueryHandler;
import oshi.util.platform.windows.WmiUtil;

/**
 * Lê o modelo da placa-mãe no Windows ({@code Win32_BaseBoard.Product}, o "Product Name" da tabela SMBIOS da
 * placa).
 *
 * <p>O OSHI 6.6.5 preenche o modelo com {@code Win32_BaseBoard.Model}, propriedade que os fabricantes quase
 * nunca preenchem; o nome da placa fica em {@code Product}. Usa o mesmo mecanismo de WMI do OSHI (sem
 * dependência nova).
 */
final class WindowsBaseboardProduct {

    private enum Property { PRODUCT }

    private static final WmiQuery<Property> QUERY = new WmiQuery<>("Win32_BaseBoard", Property.class);

    private WindowsBaseboardProduct() {
    }

    /** Valor bruto de Product, ou null se a consulta falhar ou não houver resultado. */
    static String query() {
        try {
            WmiResult<Property> result = WmiQueryHandler.createInstance().queryWMI(QUERY);
            return result.getResultCount() > 0 ? WmiUtil.getString(result, Property.PRODUCT, 0) : null;
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }
}
