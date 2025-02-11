"use client";
import React, { useEffect, useState } from "react";
import FullCalendar from "@fullcalendar/react";
import dayGridPlugin from "@fullcalendar/daygrid";
import interactionPlugin from "@fullcalendar/interaction";
import type { Calendar } from "@/lib/calendars/types/calendarTypes";
import { EventClickArg } from "@fullcalendar/core";
import { scheduleApi } from "@/lib/schedule/api/scheduleApi";
import dayjs from "dayjs";
import { useRouter } from "next/navigation";
import './CalendarView.css';

interface CalendarViewProps {
    calendars: Calendar[];
    selectedCalendar: Calendar | null;
    onCalendarSelect: (calendar: Calendar) => void;
}

export const CalendarView: React.FC<CalendarViewProps> = ({
                                                              calendars,
                                                              selectedCalendar,
                                                              onCalendarSelect,
                                                          }) => {
    const [events, setEvents] = useState<any[]>([]);
    const router = useRouter();

    useEffect(() => {
        if (!selectedCalendar) return;

        const fetchSchedules = async () => {
            try {
                const today = dayjs().format("YYYY-MM-DD");
                const fetchedSchedules = await scheduleApi.getMonthlySchedules(
                    selectedCalendar.id,
                    today
                );

                const formattedEvents = fetchedSchedules.map(schedule => ({
                    id: String(schedule.id),
                    title: schedule.title,
                    start: schedule.startTime,
                    end: schedule.endTime,
                    description: schedule.description,
                    allDay: false,
                }));

                formattedEvents.sort((a, b) => dayjs(b.start).valueOf() - dayjs(a.start).valueOf());

                setEvents(formattedEvents);
            } catch (error) {
                console.error("📛 일정 불러오기 실패:", error);
            }
        };

        fetchSchedules();
    }, [selectedCalendar]);

    const handleEventClick = (clickInfo: EventClickArg) => {
        if (selectedCalendar) {
            router.push(`/calendars/${selectedCalendar.id}/schedules/${clickInfo.event.id}`);
        } else {
            console.error("📛 선택된 캘린더가 없습니다.");
        }
    };

    if (!selectedCalendar) {
        return (
            <div className="empty-state">
                <p className="empty-state-title">새로운 캘린더를 만들어보세요!</p>
                <p className="empty-state-description">
                    좌측 메뉴에서 + NEW CALENDAR을 통해 새로운 캘린더를 만들어보세요!
                </p>
            </div>
        );
    }

    return (
        <div className="w-full h-full bg-white relative">
            <div className="h-full p-4">
                <FullCalendar
                    plugins={[dayGridPlugin, interactionPlugin]}
                    initialView="dayGridMonth"
                    headerToolbar={{
                        left: "prev,today,next",
                        center: "title",
                        right: "dayGridMonth,dayGridWeek",
                    }}
                    events={events}
                    eventClick={handleEventClick}
                    selectable={true}
                    handleWindowResize={true}
                    dayCellContent={(e) => e.dayNumberText}
                    height="100%"
                    aspectRatio={1.5}
                    contentHeight="auto"
                    dayMaxEventRows={true}
                    fixedWeekCount={false}
                    dayCellClassNames="min-h-[100px] p-2"
                    eventDisplay="block"
                    eventContent={(eventInfo) => (
                        <div className="truncate font-bold text-sm cursor-pointer text-blue-100 hover:bg-blue-400 p-1 rounded">
                            {eventInfo.event.title}
                        </div>
                    )}
                    buttonText={{
                        today: 'TODAY',
                        month: 'M',
                        week: 'W',
                    }}
                    buttonIcons={{
                        prev: 'chevron-left',
                        next: 'chevron-right',
                    }}
                    views={{
                        dayGridMonth: {
                            titleFormat: { year: 'numeric', month: 'long' },
                            dayHeaderFormat: { weekday: 'short' },
                        },
                        dayGridWeek: {
                            titleFormat: { year: 'numeric', month: 'long' },
                        },
                    }}
                />
            </div>
            <div className="px-4" style={{ marginBottom: '110px'}}>
                <button
                    onClick={() => {
                        if (selectedCalendar) {
                            router.push(`/calendars/${selectedCalendar.id}/schedules`);
                        } else {
                            console.error("📛 선택된 캘린더가 없습니다.");
                        }
                    }}
                    className="absolute bottom-40 right-4 bg-white text-black py-2 px-4 rounded-lg shadow-md hover:bg-blue-100"
                >
                    MAKE SCHEDULE
                </button>
            </div>
        </div>
    );
};