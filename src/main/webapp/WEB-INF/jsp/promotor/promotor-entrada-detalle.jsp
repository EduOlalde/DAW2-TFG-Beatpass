<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib uri="jakarta.tags.fmt" prefix="fmt" %>
<%@ taglib uri="jakarta.tags.functions" prefix="fn" %>

<c:set var="pageTitleVar" value="Editar Tipo de Entrada" />

<jsp:include page="/WEB-INF/jsp/promotor/_header.jsp">
    <jsp:param name="pageTitle" value="${pageTitleVar}" />
    <jsp:param name="currentNav" value="detalleFestival" /> 
</jsp:include>

<div class="container mx-auto p-4 md:p-8 max-w-2xl">

    <div class="flex flex-col sm:flex-row justify-between items-center mb-6 pb-4 border-b border-gray-300">
        <h1 class="text-2xl md:text-3xl font-bold text-gray-800 mb-4 sm:mb-0">Editar Tipo de Entrada</h1>
    </div>

    <div class="mb-4">
        <c:if test="${not empty entrada.idFestival}">
            <a href="${pageContext.request.contextPath}/api/promotor/festivales/ver/${entrada.idFestival}" class="text-indigo-600 hover:text-indigo-800 text-sm">&larr; Volver a Detalles del Festival</a>
        </c:if>
    </div>

    <c:if test="${not empty requestScope.error}">
        <div class="bg-red-100 border-l-4 border-red-500 text-red-700 p-4 mb-4 rounded-md shadow-sm" role="alert">
            <p class="font-bold">Error:</p> <p>${requestScope.error}</p>
        </div>
    </c:if>

    <%-- Formulario de Edición --%>
    <form action="${pageContext.request.contextPath}/api/promotor/entradas/${entrada.idEntrada}/actualizar" method="post" class="bg-white p-6 md:p-8 rounded-lg shadow-md space-y-4">
        <input type="hidden" name="idEntrada" value="${entrada.idEntrada}">
        <p class="text-sm text-gray-500">Editando entrada para Festival ID: ${entrada.idFestival}</p>

        <div>
            <label for="tipo" class="block text-sm font-medium text-gray-700 mb-1">
                Tipo <span class="text-red-500 ml-1">*</span>
            </label>
            <input type="text" id="tipo" name="tipo" value="${entrada.tipo}" required maxlength="50"
                   class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring focus:ring-indigo-500 focus:ring-opacity-50 sm:text-sm">
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
                <label for="precio" class="block text-sm font-medium text-gray-700 mb-1">
                    Precio (€) <span class="text-red-500 ml-1">*</span>
                </label>
                <input type="number" id="precio" name="precio" value="${entrada.precio}" required min="0" step="0.01"
                       class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring focus:ring-indigo-500 focus:ring-opacity-50 sm:text-sm">
            </div>
            <div>
                <label for="stock" class="block text-sm font-medium text-gray-700 mb-1">
                    Stock <span class="text-red-500 ml-1">*</span>
                </label>
                <input type="number" id="stock" name="stock" value="${entrada.stock}" required min="0" step="1"
                       class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring focus:ring-indigo-500 focus:ring-opacity-50 sm:text-sm">
            </div>
        </div>

        <div>
            <label for="descripcion" class="block text-sm font-medium text-gray-700 mb-1">Descripción (Opcional)</label>
            <textarea id="descripcion" name="descripcion" rows="3"
                      class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring focus:ring-indigo-500 focus:ring-opacity-50 sm:text-sm">${entrada.descripcion}</textarea>
        </div>

        <div class="mt-6 flex justify-end space-x-3 pt-4 border-t border-gray-200">
            <a href="${pageContext.request.contextPath}/api/promotor/festivales/ver/${entrada.idFestival}"
               class="btn btn-secondary">
                Cancelar
            </a>
            <button type="submit"
                    class="btn btn-primary">
                Guardar Cambios
            </button>
        </div>
    </form>
</div>
