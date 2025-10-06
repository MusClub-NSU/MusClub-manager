export default function Home() {
    const rows = [
        { name: "Кочанов Никита", position: "Студент" },
        { name: "Анисимов Кирилл", position: "Студент" },
        { name: "Кулешов Артемий", position: "Студент" },
        { name: "Сунтао", position: "Студент" },
    ];

    return (
        <main className="flex items-center justify-center min-h-screen p-4">
            <div className="w-full max-w-3xl">
                <h1 className="text-3xl font-bold mb-6">Сотрудники</h1>
                <div className="overflow-x-auto rounded-lg border border-[--foreground]/20">
                    <table className="min-w-full text-left">
                        <thead className="bg-[--foreground]/5">
                            <tr>
                                <th className="px-4 py-3 font-semibold">ФИО</th>
                                <th className="px-4 py-3 font-semibold">Должность</th>
                            </tr>
                        </thead>
                        <tbody>
                            {rows.map((row) => (
                                <tr key={row.name} className="border-t border-[--foreground]/10">
                                    <td className="px-4 py-3">{row.name}</td>
                                    <td className="px-4 py-3 text-[--foreground]/70">{row.position}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </div>
        </main>
    );
}
